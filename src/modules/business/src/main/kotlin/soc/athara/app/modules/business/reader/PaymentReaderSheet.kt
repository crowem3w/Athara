package soc.athara.app.modules.business.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import soc.athara.app.modules.business.R

private sealed interface ReaderResult {
    data class Token(val token: PaymentToken) : ReaderResult
    /** The tap or QR was read but is not one of our payment codes (or the tap failed half-way). */
    data object Invalid : ReaderResult
    data object ScannerUnavailable : ReaderResult
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
 * The business's "collect a payment" reader. Listens for an NFC tap (reader mode) the whole time it is open;
 * "Scan QR code" is the fallback when NFC is off, missing or fails.
 *
 * Only the token is read here. Charging the customer needs the backend payment sessions
 * (docs: phone-to-phone-payment-plan, section 7), which do not exist yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentReaderSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? ComponentActivity }
    var availability by remember { mutableStateOf(nfcAvailability(context)) }
    var result by remember { mutableStateOf<ReaderResult?>(null) }

    val reader = remember(activity) {
        activity?.let { a ->
            NfcTokenReader(a) { raw ->
                if (result == null) {
                    val token = PaymentTokenParser.parse(raw)
                    result = if (token != null) ReaderResult.Token(token) else ReaderResult.Invalid
                }
            }
        }
    }

    // Reader mode only while this screen is in the foreground.
    DisposableEffect(activity, reader) {
        if (activity == null || reader == null) return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    availability = nfcAvailability(context)
                    if (result == null) reader.enable()
                }
                Lifecycle.Event.ON_PAUSE -> reader.disable()
                else -> Unit
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose {
            activity.lifecycle.removeObserver(observer)
            reader.disable()
        }
    }

    // Stop listening once something was read; listen again after "Read another".
    LaunchedEffect(result, availability) {
        val foreground = activity?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
        if (result == null && availability == NfcAvailability.ENABLED && foreground) reader?.enable() else reader?.disable()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.business_reader_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            when (val r = result) {
                null -> {
                    Text(
                        stringResource(
                            when (availability) {
                                NfcAvailability.ENABLED -> R.string.business_reader_hold
                                NfcAvailability.DISABLED -> R.string.business_reader_nfc_off
                                NfcAvailability.UNSUPPORTED -> R.string.business_reader_nfc_missing
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (availability == NfcAvailability.DISABLED) {
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.business_reader_open_nfc_settings)) }
                    }
                    OutlinedButton(
                        onClick = {
                            startQrScan(
                                activity,
                                onScanned = { raw ->
                                    val token = PaymentTokenParser.parse(raw)
                                    result = if (token != null) ReaderResult.Token(token) else ReaderResult.Invalid
                                },
                                onFailed = { result = ReaderResult.ScannerUnavailable },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                        Text(stringResource(R.string.business_reader_scan_qr), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                is ReaderResult.Token -> {
                    Text(stringResource(R.string.business_reader_read_ok), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.business_reader_session, r.token.sessionId.take(8)), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.business_reader_not_connected), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { result = null }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.business_reader_read_another))
                    }
                }
                ReaderResult.Invalid -> {
                    Text(stringResource(R.string.business_reader_invalid), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { result = null }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.business_reader_try_again))
                    }
                }
                ReaderResult.ScannerUnavailable -> {
                    Text(stringResource(R.string.business_reader_scanner_unavailable), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { result = null }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.business_reader_try_again))
                    }
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.business_action_cancel))
            }
        }
    }
}

/** Opens Google's code scanner (no camera permission needed; needs Google Play services). Closing it is not a failure. */
private fun startQrScan(activity: Activity?, onScanned: (String?) -> Unit, onFailed: () -> Unit) {
    if (activity == null) {
        onFailed()
        return
    }
    val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    GmsBarcodeScanning.getClient(activity, options).startScan()
        .addOnSuccessListener { barcode -> onScanned(barcode.rawValue) }
        .addOnCanceledListener { /* user closed the scanner */ }
        .addOnFailureListener { onFailed() }
}
