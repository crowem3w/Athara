package soc.athara.app.modules.personal.home.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import soc.athara.app.modules.personal.R
import soc.athara.app.shared.payments.PaymentError
import soc.athara.app.shared.payments.PaymentResult
import soc.athara.app.shared.payments.PaymentStatus
import soc.athara.app.shared.payments.Payments
import soc.athara.app.shared.payments.ProximityIntent
import soc.athara.app.shared.payments.ble.BeaconAdvertiser
import soc.athara.app.shared.payments.ble.BeaconCodec
import soc.athara.app.shared.payments.ble.BeaconScanner
import soc.athara.app.shared.payments.ble.BlePermissions
import soc.athara.app.shared.payments.ble.BleState
import soc.athara.app.shared.payments.ble.ProximityTuning
import soc.athara.app.shared.payments.ble.RssiTracker

/** Width / height of `personal_nfc_card` (941 x 1672), so the card keeps its portrait shape on any screen. */
private const val BleCardAspect = 941f / 1672f

private val BleCardShape = RoundedCornerShape(28.dp)

/** What the card tells the person. */
private enum class Phase { Searching, MoveCloser, Connecting, Holding, HoldingReduced, Claimed }

/**
 * The payer's screen for a Bluetooth payment, used on phones where NFC is missing or off.
 *
 *  1. Scans for the business's beacon and watches its signal for about a second.
 *  2. When one business clearly stands out, creates the payment on the server (amount fixed here), bound to that business.
 *  3. Broadcasts the single-use payer beacon so the business phone can measure it, and waits.
 *  4. Polls the server until the business accepts or declines, or it expires.
 *
 * Both phones must stay in front with the screen on, so leaving the screen (or the X / back) cancels the payment.
 * A phone that cannot broadcast runs in reduced mode: the business sees the request in its inbox and confirms in person.
 * Only the server moves money.
 */
@Composable
internal fun BlePayCard(amountMinor: Long, onClose: () -> Unit, onOutcome: (PayOutcome) -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val latestOutcome by rememberUpdatedState(onOutcome)
    val latestClose by rememberUpdatedState(onClose)

    BackHandler(onBack = onClose)

    var bleState by remember { mutableStateOf(BlePermissions.state(context)) }
    var phase by remember { mutableStateOf(Phase.Searching) }
    var merchantName by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        bleState = BlePermissions.state(context)
    }

    // Keep the screen on, re-check Bluetooth when coming back from Settings, and cancel when the screen is left
    // (Android limits Bluetooth in the background, and a payment must not sit half-open).
    val lifecycleOwner = remember(context) { context.findComponentActivity() }
    DisposableEffect(lifecycleOwner) {
        view.keepScreenOn = true
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> bleState = BlePermissions.state(context)
                Lifecycle.Event.ON_STOP -> latestClose()
                else -> Unit
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            view.keepScreenOn = false
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (BlePermissions.state(context) == BleState.NEEDS_PERMISSION) permissionLauncher.launch(BlePermissions.required())
    }

    // The whole payment runs here, and starts (or restarts) only when Bluetooth is ready.
    LaunchedEffect(bleState == BleState.READY) {
        if (bleState != BleState.READY) return@LaunchedEffect
        phase = Phase.Searching
        runBlePayment(
            context = context,
            amountMinor = amountMinor,
            setPhase = { phase = it },
            setMerchant = { merchantName = it },
            onCompleted = { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) },
            onOutcome = { latestOutcome(it) },
        )
    }

    // Dim layer. Swallows touches so the blurred home screen behind cannot be used.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            val reserved = 140.dp // room under the card for the message and a button
            val cardWidth = minOf(maxWidth * 0.78f, (maxHeight - reserved) * BleCardAspect)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(modifier = Modifier.width(cardWidth)) {
                    Image(
                        painter = painterResource(R.drawable.personal_nfc_card),
                        contentDescription = stringResource(R.string.personal_ble_card_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(cardWidth / BleCardAspect).clip(BleCardShape),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 20.dp, end = 20.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable(role = Role.Button, onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.personal_card_close),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (bleState != BleState.READY) {
                    BlockedNotice(bleState, context) { permissionLauncher.launch(BlePermissions.required()) }
                } else {
                    PhaseNotice(phase, merchantName)
                }
            }
        }
    }
}

@Composable
private fun PhaseNotice(phase: Phase, merchantName: String?) {
    val busy = phase == Phase.Searching || phase == Phase.Connecting || phase == Phase.Claimed
    if (busy) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        Spacer(Modifier.height(8.dp))
    }
    val text = when (phase) {
        Phase.Searching -> stringResource(R.string.personal_ble_searching)
        Phase.MoveCloser -> stringResource(R.string.personal_ble_move_closer)
        Phase.Connecting -> stringResource(R.string.personal_ble_connecting)
        Phase.Holding ->
            if (merchantName != null) stringResource(R.string.personal_ble_holding_to, merchantName) else stringResource(R.string.personal_ble_holding)
        Phase.HoldingReduced ->
            if (merchantName != null) stringResource(R.string.personal_ble_reduced_to, merchantName) else stringResource(R.string.personal_ble_reduced)
        Phase.Claimed -> stringResource(R.string.personal_card_waiting)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BlockedNotice(state: BleState, context: Context, requestPermission: () -> Unit) {
    val message = when (state) {
        BleState.UNSUPPORTED -> R.string.personal_ble_unsupported
        BleState.OFF -> R.string.personal_ble_off
        BleState.NEEDS_PERMISSION -> R.string.personal_ble_needs_permission
        BleState.LOCATION_OFF -> R.string.personal_ble_location_off
        BleState.READY -> return
    }
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    val action: Pair<Int, () -> Unit>? = when (state) {
        BleState.NEEDS_PERMISSION -> R.string.personal_ble_allow to requestPermission
        BleState.OFF -> R.string.personal_ble_open_settings to { openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS) }
        BleState.LOCATION_OFF -> R.string.personal_ble_open_location to { openSettings(context, Settings.ACTION_LOCATION_SOURCE_SETTINGS) }
        else -> null
    }
    if (action != null) {
        TextButton(onClick = action.second) { Text(stringResource(action.first), color = Color.White) }
    }
}

private fun Context.findComponentActivity(): ComponentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c as? ComponentActivity
        c = c.baseContext
    }
    return null
}

private fun openSettings(context: Context, action: String) {
    context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * The payer's side of the payment, start to finish. Returns when an outcome was reported or the caller cancelled it.
 * Everything it starts (scan, broadcast, the server request) is stopped or cancelled in `finally`, so leaving
 * the card at any moment cannot leave a claimable request behind. Runs on the main thread (a LaunchedEffect),
 * which is also where the scanner delivers readings, so the tracker needs no locking.
 */
private suspend fun runBlePayment(
    context: Context,
    amountMinor: Long,
    setPhase: (Phase) -> Unit,
    setMerchant: (String?) -> Unit,
    onCompleted: () -> Unit,
    onOutcome: (PayOutcome) -> Unit,
) {
    val scanner = BeaconScanner(context, BeaconCodec.TYPE_MERCHANT)
    val advertiser = BeaconAdvertiser(context)
    val tracker = RssiTracker()
    val heard = HashMap<String, BeaconCodec.Heard.Merchant>() // handle -> the newest beacon from that business
    val reduced = !advertiser.isSupported()
    var intentId: String? = null
    var finished = false

    fun finish(outcome: PayOutcome) {
        finished = true
        onOutcome(outcome)
    }

    try {
        val started = scanner.start(
            onHeard = { beacon, rssi ->
                if (beacon is BeaconCodec.Heard.Merchant) {
                    heard[beacon.handle] = beacon
                    tracker.add(beacon.handle, rssi, SystemClock.elapsedRealtime())
                }
            },
            onFailed = { tracker.clear() },
        )
        if (!started) return finish(PayOutcome.Failed(PaymentError.OTHER))

        // ---- 1-2. find the business and create the payment ----
        setPhase(Phase.Searching)
        val intent: ProximityIntent = withTimeoutOrNull(ProximityTuning.SEARCH_TIMEOUT_MS) {
            var created: ProximityIntent? = null
            while (created == null) {
                delay(ProximityTuning.TICK_MS)
                when (val pick = tracker.pick(SystemClock.elapsedRealtime(), ProximityTuning.CLIENT_FLOOR_DBM)) {
                    RssiTracker.Pick.None, is RssiTracker.Pick.Settling -> setPhase(Phase.Searching)
                    is RssiTracker.Pick.Ambiguous -> setPhase(Phase.MoveCloser)
                    is RssiTracker.Pick.Clear -> {
                        val beacon = heard[pick.best.key] ?: continue
                        setPhase(Phase.Connecting)
                        when (val r = Payments.createIntent(amountMinor, beacon.handle, beacon.nonce, pick.best.medianDbm, reduced)) {
                            is PaymentResult.Ok -> created = r.value
                            is PaymentResult.Failed -> when (r.error) {
                                // The server says the phones are not close enough. Collect fresh readings and ask the person to come closer.
                                PaymentError.TOO_FAR -> { tracker.clear(); setPhase(Phase.MoveCloser); delay(500) }
                                // The nonce changed while we were reading it: wait for the next beacon.
                                PaymentError.INVALID_TOKEN -> { tracker.clear(); setPhase(Phase.Searching); delay(500) }
                                else -> { finish(PayOutcome.Failed(r.error)); return@withTimeoutOrNull null }
                            }
                        }
                    }
                }
            }
            created
        } ?: run {
            if (!finished) finish(PayOutcome.NoBusinessFound)
            return
        }
        intentId = intent.intentId
        scanner.stop() // heard enough: save battery
        setMerchant(intent.merchantName)

        // ---- 3. broadcast our single-use id (unless this phone cannot) ----
        if (reduced) {
            setPhase(Phase.HoldingReduced)
        } else {
            val payload = intent.beaconId?.let { BeaconCodec.payerPayload(it) }
            if (payload == null || !advertiser.start(payload)) return finish(PayOutcome.Failed(PaymentError.OTHER))
            setPhase(Phase.Holding)
        }

        // ---- 4. wait for the business ----
        while (true) {
            delay(ProximityTuning.POLL_MS)
            val r = Payments.intentStatus(intent.intentId) as? PaymentResult.Ok ?: continue
            when (r.value.status) {
                PaymentStatus.CREATED -> Unit
                PaymentStatus.CLAIMED -> {
                    setPhase(Phase.Claimed)
                    advertiser.stop() // the id is spent
                }
                PaymentStatus.COMPLETED -> { onCompleted(); return finish(PayOutcome.Completed(r.value.amountMinor)) }
                PaymentStatus.CANCELLED -> return finish(PayOutcome.Cancelled)
                PaymentStatus.EXPIRED -> return finish(PayOutcome.Expired)
            }
        }
    } finally {
        scanner.stop()
        advertiser.stop()
        // Left before it finished (X, back, screen off): cancel so the request cannot be claimed later.
        val id = intentId
        if (id != null && !finished) withContext(NonCancellable) { Payments.cancelIntent(id) }
    }
}
