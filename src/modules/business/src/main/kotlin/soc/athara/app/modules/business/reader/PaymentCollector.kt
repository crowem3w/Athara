package soc.athara.app.modules.business.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import soc.athara.app.modules.business.R
import soc.athara.app.shared.designsystem.theme.Teal
import soc.athara.app.shared.payments.PaymentError
import soc.athara.app.shared.payments.PaymentResult
import soc.athara.app.shared.payments.PaymentSession
import soc.athara.app.shared.payments.Payments

private val SuccessGreen = Color(0xFF2E9E5B)

/** Where the collection is: waiting for a tap, talking to the server, or showing a dialog. */
private sealed interface Step {
    data object Idle : Step
    /** Token read; asking the server to claim it. */
    data object Claiming : Step
    /** The server returned the amount; waiting for Accept or Cancel. */
    data class Review(val session: PaymentSession) : Step
    data class Accepting(val session: PaymentSession) : Step
    data class Done(val session: PaymentSession) : Step
    data class Problem(val message: Int, val offerNfcSettings: Boolean = false) : Step
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * The business's tap-to-collect flow. There is no panel: the centre button just turns scanning on.
 *
 * While [scanning] is on, NFC reader mode listens (only while the app is in front). When a customer's card is
 * read, the token is claimed on the server, which answers with the amount. A dialog then shows it with
 * Accept and Cancel. Accept makes the server move the money; Cancel releases the request.
 * Scanning switches itself off after a result so the next tap is deliberate ([onScanningChange] false).
 *
 * [pillModifier] positions the small "Ready to scan" pill (the caller places it above the nav bar).
 */
@Composable
internal fun PaymentCollector(
    scanning: Boolean,
    onScanningChange: (Boolean) -> Unit,
    pillModifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() as? ComponentActivity }
    val scope = rememberCoroutineScope()
    val setScanning by rememberUpdatedState(onScanningChange)
    var availability by remember { mutableStateOf(nfcAvailability(context)) }
    var step by remember { mutableStateOf<Step>(Step.Idle) }

    LaunchedEffect(scanning) { if (scanning) availability = nfcAvailability(context) }

    fun onRead(raw: String?) {
        if (step != Step.Idle) return
        val token = PaymentTokenParser.parse(raw)
        if (token == null) {
            step = Step.Problem(R.string.business_pay_err_invalid)
            return
        }
        step = Step.Claiming
        scope.launch {
            step = when (val r = Payments.claim(token.sessionId, token.nonce)) {
                is PaymentResult.Ok -> Step.Review(r.value)
                is PaymentResult.Failed -> Step.Problem(messageFor(r.error))
            }
        }
    }

    val reader = remember(activity) { activity?.let { a -> NfcTokenReader(a) { raw -> onRead(raw) } } }
    val listening = scanning && step == Step.Idle && availability == NfcAvailability.ENABLED

    // Reader mode only while we want a tap and the app is in the foreground.
    DisposableEffect(activity, reader, listening) {
        if (activity == null || reader == null) return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    availability = nfcAvailability(context)
                    if (listening) reader.enable()
                }
                Lifecycle.Event.ON_PAUSE -> reader.disable()
                else -> Unit
            }
        }
        activity.lifecycle.addObserver(observer)
        if (listening && activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) reader.enable() else reader.disable()
        onDispose {
            activity.lifecycle.removeObserver(observer)
            reader.disable()
        }
    }

    // ---- the "Ready to scan" pill (not a panel: it does not block the screen) ----
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
                Text(stringResource(R.string.business_pay_ready), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = { setScanning(false) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.business_pay_stop), tint = Color.White)
                }
            }
        }
    }

    // ---- NFC cannot be used ----
    if (scanning && availability != NfcAvailability.ENABLED && step == Step.Idle) {
        val missing = availability == NfcAvailability.UNSUPPORTED
        AlertDialog(
            onDismissRequest = { setScanning(false) },
            title = { Text(stringResource(R.string.business_pay_nfc_title)) },
            text = { Text(stringResource(if (missing) R.string.business_pay_nfc_missing else R.string.business_pay_nfc_off)) },
            confirmButton = {
                if (missing) {
                    TextButton(onClick = { setScanning(false) }) { Text(stringResource(R.string.business_pay_ok)) }
                } else {
                    TextButton(onClick = {
                        setScanning(false)
                        context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }) { Text(stringResource(R.string.business_pay_open_nfc_settings)) }
                }
            },
            dismissButton = {
                if (!missing) TextButton(onClick = { setScanning(false) }) { Text(stringResource(R.string.business_action_cancel)) }
            },
        )
    }

    // ---- dialogs for each step ----
    when (val s = step) {
        Step.Idle -> Unit

        Step.Claiming -> AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.business_pay_checking)) },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
            confirmButton = {},
        )

        is Step.Review, is Step.Accepting -> {
            val session = if (s is Step.Review) s.session else (s as Step.Accepting).session
            val busy = s is Step.Accepting
            AlertDialog(
                // Back or a tap outside counts as Cancel, so a claimed payment is never left hanging.
                onDismissRequest = {
                    if (!busy) {
                        scope.launch { Payments.cancel(session.sessionId) }
                        step = Step.Idle
                        setScanning(false)
                    }
                },
                properties = DialogProperties(dismissOnClickOutside = false),
                title = { Text(stringResource(R.string.business_pay_request_title)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatPeso(session.amountMinor),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        session.payerName?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.business_pay_from, it), style = MaterialTheme.typography.bodyLarge)
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
                            step = Step.Accepting(session)
                            scope.launch {
                                step = when (val r = Payments.accept(session.sessionId)) {
                                    is PaymentResult.Ok -> {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        Step.Done(r.value)
                                    }
                                    is PaymentResult.Failed -> Step.Problem(messageFor(r.error))
                                }
                            }
                        },
                    ) { Text(stringResource(R.string.business_pay_accept)) }
                },
                dismissButton = {
                    TextButton(
                        enabled = !busy,
                        onClick = {
                            scope.launch { Payments.cancel(session.sessionId) }
                            step = Step.Idle
                            setScanning(false)
                        },
                    ) { Text(stringResource(R.string.business_action_cancel)) }
                },
            )
        }

        is Step.Done -> AlertDialog(
            onDismissRequest = { step = Step.Idle; setScanning(false) },
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
                    Text(formatPeso(s.session.amountMinor), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.business_pay_done_body), style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(onClick = { step = Step.Idle; setScanning(false) }) { Text(stringResource(R.string.business_pay_ok)) }
            },
        )

        is Step.Problem -> AlertDialog(
            onDismissRequest = { step = Step.Idle; setScanning(false) },
            title = { Text(stringResource(R.string.business_pay_problem_title)) },
            text = { Text(stringResource(s.message), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = { step = Step.Idle; setScanning(false) }) { Text(stringResource(R.string.business_pay_ok)) }
            },
        )
    }
}

private fun messageFor(error: PaymentError): Int = when (error) {
    PaymentError.INVALID_TOKEN -> R.string.business_pay_err_token
    PaymentError.SESSION_NOT_OPEN -> R.string.business_pay_err_closed
    PaymentError.NOT_ALLOWED -> R.string.business_pay_err_not_allowed
    PaymentError.INSUFFICIENT_FUNDS -> R.string.business_pay_err_funds
    PaymentError.NETWORK -> R.string.business_pay_err_network
    PaymentError.UNAUTHORIZED -> R.string.business_pay_err_signin
    PaymentError.RATE_LIMITED -> R.string.business_pay_err_rate
    PaymentError.NOT_FOUND, PaymentError.OTHER -> R.string.business_pay_err_other
}

/** 1250 -> "₱12.50". Integer math only. */
private fun formatPeso(minor: Long): String = "₱%d.%02d".format(minor / 100, minor % 100)
