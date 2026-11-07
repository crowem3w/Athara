package soc.athara.app.modules.business.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import soc.athara.app.modules.business.R
import soc.athara.app.shared.designsystem.theme.Teal
import soc.athara.app.shared.payments.PaymentError
import soc.athara.app.shared.payments.PaymentResult
import soc.athara.app.shared.payments.Payments
import soc.athara.app.shared.payments.Proximity
import soc.athara.app.shared.payments.ProximityIntent
import soc.athara.app.shared.payments.ble.BeaconAdvertiser
import soc.athara.app.shared.payments.ble.BeaconCodec
import soc.athara.app.shared.payments.ble.BeaconScanner
import soc.athara.app.shared.payments.ble.BlePermissions
import soc.athara.app.shared.payments.ble.BleState
import soc.athara.app.shared.payments.ble.ProximityTuning
import soc.athara.app.shared.payments.ble.RssiTracker

private val SuccessGreen = Color(0xFF2E9E5B)
private val WarningAmber = Color(0xFFB26A00)

private sealed interface BleStep {
    data object Idle : BleStep
    
    data class Review(val intent: ProximityIntent) : BleStep
    data class Accepting(val intent: ProximityIntent) : BleStep
    data class Done(val intent: ProximityIntent) : BleStep
    data class Problem(val message: Int) : BleStep
}

private fun Context.findComponentActivity(): ComponentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c as? ComponentActivity
        c = c.baseContext
    }
    return null
}














@Composable
internal fun BleCollector(
    scanning: Boolean,
    onScanningChange: (Boolean) -> Unit,
    pillModifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findComponentActivity() }
    val scope = rememberCoroutineScope()
    val setScanning by rememberUpdatedState(onScanningChange)

    var bleState by remember { mutableStateOf(BlePermissions.state(context)) }
    var step by remember { mutableStateOf<BleStep>(BleStep.Idle) }
    var hint by remember { mutableStateOf<Int?>(null) }
    var hintSetAtMs by remember { mutableStateOf(0L) }
    var checking by remember { mutableStateOf(false) }
    val currentStep by rememberUpdatedState(step)
    
    val ignoredUntil = remember { HashMap<String, Long>() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        bleState = BlePermissions.state(context)
    }

    LaunchedEffect(scanning) {
        if (!scanning) return@LaunchedEffect
        bleState = BlePermissions.state(context)
        if (bleState == BleState.NEEDS_PERMISSION) permissionLauncher.launch(BlePermissions.required())
    }

    
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> bleState = BlePermissions.state(context)
                Lifecycle.Event.ON_STOP -> if (currentStep == BleStep.Idle) setScanning(false)
                else -> Unit
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    val listening = scanning && step == BleStep.Idle && bleState == BleState.READY

    DisposableEffect(listening) {
        view.keepScreenOn = listening
        onDispose { view.keepScreenOn = false }
    }

    
    fun attemptClaim(key: String, call: suspend () -> PaymentResult<ProximityIntent>) {
        if (checking || step != BleStep.Idle) return
        checking = true
        hint = null
        scope.launch {
            val result = call()
            checking = false
            when (result) {
                is PaymentResult.Ok -> {
                    hint = null
                    step = BleStep.Review(result.value)
                }
                is PaymentResult.Failed -> when (result.error) {
                    
                    PaymentError.TOO_FAR -> {
                        hint = R.string.business_ble_hint_closer
                        hintSetAtMs = SystemClock.elapsedRealtime()
                        ignoredUntil[key] = SystemClock.elapsedRealtime() + 1_000
                    }
                    
                    PaymentError.INVALID_TOKEN, PaymentError.SESSION_NOT_OPEN ->
                        ignoredUntil[key] = SystemClock.elapsedRealtime() + ProximityTuning.IGNORE_BEACON_MS
                    else -> step = BleStep.Problem(bleMessageFor(result.error))
                }
            }
        }
    }

    
    LaunchedEffect(listening) {
        if (!listening) return@LaunchedEffect
        hint = null
        val advertiser = BeaconAdvertiser(context)
        val scanner = BeaconScanner(context, BeaconCodec.TYPE_PAYER)
        val tracker = RssiTracker()
        try {
            if (!advertiser.isSupported()) {
                step = BleStep.Problem(R.string.business_ble_err_advertise_unsupported)
                return@LaunchedEffect
            }
            val started = scanner.start(
                onHeard = { beacon, rssi ->
                    if (beacon is BeaconCodec.Heard.Payer) tracker.add(beacon.beaconId, rssi, SystemClock.elapsedRealtime())
                },
                onFailed = { tracker.clear() },
            )
            if (!started) {
                step = BleStep.Problem(R.string.business_ble_err_start)
                return@LaunchedEffect
            }
            coroutineScope {
                
                launch {
                    while (true) {
                        when (val r = Payments.proximityBeacon()) {
                            is PaymentResult.Ok -> {
                                val payload = BeaconCodec.merchantPayload(r.value.handle, r.value.nonce)
                                if (payload == null || !advertiser.start(payload)) {
                                    step = BleStep.Problem(R.string.business_ble_err_advertise)
                                    return@launch
                                }
                                val seconds = (r.value.ttlSeconds - ProximityTuning.NONCE_REFRESH_MARGIN_SECONDS).coerceAtLeast(2)
                                delay(seconds * 1_000L)
                            }
                            is PaymentResult.Failed -> {
                                if (r.error == PaymentError.UNAUTHORIZED || r.error == PaymentError.NOT_ALLOWED) {
                                    step = BleStep.Problem(bleMessageFor(r.error))
                                    return@launch
                                }
                                delay(2_000) 
                            }
                        }
                    }
                }
                
                launch {
                    while (true) {
                        delay(ProximityTuning.TICK_MS)
                        if (checking) continue
                        val now = SystemClock.elapsedRealtime()
                        ignoredUntil.entries.removeAll { it.value <= now }
                        
                        if (hint != null && now - hintSetAtMs > 4_000) hint = null
                        when (val pick = tracker.pick(now, ProximityTuning.CLIENT_FLOOR_DBM, exclude = { ignoredUntil.containsKey(it) })) {
                            RssiTracker.Pick.None, is RssiTracker.Pick.Settling -> Unit
                            is RssiTracker.Pick.Ambiguous -> {
                                hint = R.string.business_ble_hint_several
                                hintSetAtMs = now
                            }
                            is RssiTracker.Pick.Clear -> attemptClaim(pick.best.key) {
                                Payments.claimIntent(pick.best.key, pick.best.medianDbm)
                            }
                        }
                    }
                }
                
                launch {
                    while (true) {
                        delay(ProximityTuning.INBOX_POLL_MS)
                        if (checking) continue
                        val waiting = (Payments.proximityInbox() as? PaymentResult.Ok)?.value
                            ?.firstOrNull { !ignoredUntil.containsKey(it.intentId) } ?: continue
                        attemptClaim(waiting.intentId) { Payments.claimIntentById(waiting.intentId) }
                    }
                }
            }
        } finally {
            scanner.stop()
            advertiser.stop()
        }
    }

    
    if (listening) {
        Surface(
            modifier = pillModifier,
            shape = CircleShape,
            color = Teal,
            contentColor = Color.White,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(hint ?: R.string.business_ble_ready), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = { setScanning(false) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.business_pay_stop), tint = Color.White)
                }
            }
        }
    }

    
    if (scanning && bleState != BleState.READY && step == BleStep.Idle) {
        val message = when (bleState) {
            BleState.UNSUPPORTED -> R.string.business_ble_unsupported
            BleState.OFF -> R.string.business_ble_off
            BleState.LOCATION_OFF -> R.string.business_ble_location_off
            else -> R.string.business_ble_needs_permission
        }
        AlertDialog(
            onDismissRequest = { setScanning(false) },
            title = { Text(stringResource(R.string.business_ble_title)) },
            text = { Text(stringResource(message)) },
            confirmButton = {
                when (bleState) {
                    BleState.UNSUPPORTED -> TextButton(onClick = { setScanning(false) }) { Text(stringResource(R.string.business_pay_ok)) }
                    BleState.OFF -> TextButton(onClick = {
                        setScanning(false)
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text(stringResource(R.string.business_ble_open_settings)) }
                    BleState.LOCATION_OFF -> TextButton(onClick = {
                        setScanning(false)
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text(stringResource(R.string.business_ble_open_location)) }
                    else -> TextButton(onClick = { permissionLauncher.launch(BlePermissions.required()) }) {
                        Text(stringResource(R.string.business_ble_allow))
                    }
                }
            },
            dismissButton = {
                if (bleState != BleState.UNSUPPORTED) TextButton(onClick = { setScanning(false) }) { Text(stringResource(R.string.business_action_cancel)) }
            },
        )
    }

    
    if (checking) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.business_pay_checking)) },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
            confirmButton = {},
        )
    }

    
    when (val s = step) {
        BleStep.Idle -> Unit

        is BleStep.Review, is BleStep.Accepting -> {
            val intent = if (s is BleStep.Review) s.intent else (s as BleStep.Accepting).intent
            val busy = s is BleStep.Accepting
            val unsure = intent.proximity == Proximity.BORDERLINE || intent.reducedMode
            AlertDialog(
                
                onDismissRequest = {
                    if (!busy) {
                        scope.launch { Payments.declineIntent(intent.intentId) }
                        step = BleStep.Idle
                        setScanning(false)
                    }
                },
                properties = DialogProperties(dismissOnClickOutside = false),
                title = { Text(stringResource(R.string.business_pay_request_title)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatBlePeso(intent.amountMinor),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        intent.payerName?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.business_pay_from, it), style = MaterialTheme.typography.bodyLarge)
                        }
                        if (unsure) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = stringResource(
                                    if (intent.reducedMode) R.string.business_ble_warn_reduced else R.string.business_ble_warn_borderline,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarningAmber,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (busy) {
                            Spacer(Modifier.height(16.dp))
                            CircularProgressIndicator(Modifier.size(28.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            step = BleStep.Accepting(intent)
                            scope.launch {
                                step = when (val r = Payments.acceptIntent(intent.intentId)) {
                                    is PaymentResult.Ok -> {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        BleStep.Done(r.value)
                                    }
                                    is PaymentResult.Failed -> BleStep.Problem(bleMessageFor(r.error))
                                }
                            }
                        },
                    ) { Text(stringResource(R.string.business_pay_accept)) }
                },
                dismissButton = {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            scope.launch { Payments.declineIntent(intent.intentId) }
                            step = BleStep.Idle
                            setScanning(false)
                        },
                    ) { Text(stringResource(R.string.business_action_cancel)) }
                },
            )
        }

        is BleStep.Done -> AlertDialog(
            onDismissRequest = { step = BleStep.Idle; setScanning(false) },
            title = { Text(stringResource(R.string.business_pay_done_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(SuccessGreen),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(formatBlePeso(s.intent.amountMinor), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.business_pay_done_body), style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(onClick = { step = BleStep.Idle; setScanning(false) }) { Text(stringResource(R.string.business_pay_ok)) }
            },
        )

        is BleStep.Problem -> AlertDialog(
            onDismissRequest = { step = BleStep.Idle; setScanning(false) },
            title = { Text(stringResource(R.string.business_pay_problem_title)) },
            text = { Text(stringResource(s.message), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = { step = BleStep.Idle; setScanning(false) }) { Text(stringResource(R.string.business_pay_ok)) }
            },
        )
    }
}

private fun bleMessageFor(error: PaymentError): Int = when (error) {
    PaymentError.INVALID_TOKEN -> R.string.business_pay_err_token
    PaymentError.SESSION_NOT_OPEN -> R.string.business_pay_err_closed
    PaymentError.NOT_ALLOWED -> R.string.business_pay_err_not_allowed
    PaymentError.INSUFFICIENT_FUNDS -> R.string.business_pay_err_funds
    PaymentError.NETWORK -> R.string.business_pay_err_network
    PaymentError.UNAUTHORIZED -> R.string.business_pay_err_signin
    PaymentError.RATE_LIMITED -> R.string.business_pay_err_rate
    PaymentError.TOO_FAR -> R.string.business_ble_hint_closer
    PaymentError.AMOUNT_OVER_LIMIT, PaymentError.DAILY_LIMIT -> R.string.business_ble_err_limit
    PaymentError.NOT_FOUND, PaymentError.OTHER -> R.string.business_pay_err_other
}


private fun formatBlePeso(minor: Long): String = "₱%d.%02d".format(minor / 100, minor % 100)
