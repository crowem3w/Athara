package soc.athara.app.modules.personal.home.components

import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import soc.athara.app.modules.personal.R
import soc.athara.app.modules.personal.nfc.PaymentToken
import soc.athara.app.modules.personal.nfc.PaymentTokenHost
import soc.athara.app.modules.personal.nfc.TokenLifetimeSeconds
import soc.athara.app.shared.payments.PaymentError
import soc.athara.app.shared.payments.PaymentResult
import soc.athara.app.shared.payments.PaymentStatus
import soc.athara.app.shared.payments.Payments

/** Width / height of `personal_nfc_card` (941 x 1672), so the card keeps its portrait shape on any screen. */
private const val CardAspect = 941f / 1672f

private val CardShape = RoundedCornerShape(28.dp)

private const val PollMillis = 1_000L

/** How a payment ended. The home screen shows the matching dialog. */
internal sealed interface PayOutcome {
    data class Completed(val amountMinor: Long) : PayOutcome
    /** The business (or this phone, from another screen) cancelled before it completed. */
    data object Cancelled : PayOutcome
    data object Expired : PayOutcome
    /** Bluetooth: no business beacon was found in time. */
    data object NoBusinessFound : PayOutcome
    data class Failed(val error: PaymentError) : PayOutcome
}

/**
 * The payer's digital card, shown over the (blurred) home screen after Pay.
 *
 * Opening it asks the server for a payment session for [amountMinor]. While it is open the phone serves
 * the session's token over NFC ([PaymentTokenHost]). The line under the card shrinks over [TokenLifetimeSeconds]
 * seconds, then the server replaces the nonce and the line resets, so a token that is read or photographed
 * becomes useless quickly. When a business claims the token the line is replaced by a waiting note; the
 * business then accepts (the server moves the money) or cancels.
 * The card polls the server; on a final status it reports a [PayOutcome] (with a confirm haptic when completed).
 * Closing the card early (X or back) cancels the session. Only the server moves money.
 */
@Composable
internal fun NfcCard(amountMinor: Long, onClose: () -> Unit, onOutcome: (PayOutcome) -> Unit) {
    val view = LocalView.current
    val context = LocalContext.current
    val latestOutcome by rememberUpdatedState(onOutcome)

    BackHandler(onBack = onClose)
    DisposableEffect(Unit) { onDispose { PaymentTokenHost.clear() } }

    var ready by remember { mutableStateOf(false) }
    var claimed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        val created = Payments.createSession(amountMinor)
        if (created is PaymentResult.Failed) {
            latestOutcome(PayOutcome.Failed(created.error))
            return@LaunchedEffect
        }
        val session = (created as PaymentResult.Ok).value
        val sessionId = session.sessionId
        var finished = false
        try {
            PaymentTokenHost.set(PaymentToken(sessionId, session.nonce.orEmpty()))
            ready = true
            coroutineScope {
                // Replace the nonce every TokenLifetimeSeconds; the line shows the time left.
                val rotation = launch {
                    while (true) {
                        progress.snapTo(1f)
                        progress.animateTo(0f, tween(durationMillis = TokenLifetimeSeconds * 1000, easing = LinearEasing))
                        when (val r = Payments.rotate(sessionId)) {
                            is PaymentResult.Ok -> r.value.nonce?.let { PaymentTokenHost.set(PaymentToken(sessionId, it)) }
                            is PaymentResult.Failed ->
                                // Not open any more: the poller below reports why. Anything else: try again next round.
                                if (r.error == PaymentError.SESSION_NOT_OPEN || r.error == PaymentError.NOT_FOUND) return@launch
                        }
                    }
                }
                // Ask the server what happened to the payment.
                launch {
                    while (true) {
                        delay(PollMillis)
                        val r = Payments.status(sessionId) as? PaymentResult.Ok ?: continue
                        when (r.value.status) {
                            PaymentStatus.CLAIMED -> if (!claimed) {
                                claimed = true
                                rotation.cancel()
                            }
                            PaymentStatus.COMPLETED -> {
                                finished = true
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                latestOutcome(PayOutcome.Completed(r.value.amountMinor))
                            }
                            PaymentStatus.CANCELLED -> { finished = true; latestOutcome(PayOutcome.Cancelled) }
                            PaymentStatus.EXPIRED -> { finished = true; latestOutcome(PayOutcome.Expired) }
                            PaymentStatus.CREATED -> Unit
                        }
                        if (finished) {
                            rotation.cancel()
                            return@launch
                        }
                    }
                }
            }
        } finally {
            PaymentTokenHost.clear()
            // Left the card before it finished (X, back, rotation): cancel so the request cannot be claimed later.
            if (!finished) withContext(NonCancellable) { Payments.cancel(sessionId) }
        }
    }

    val nfcOn = remember { NfcAdapter.getDefaultAdapter(context)?.isEnabled == true }
    val nfcMissing = remember { NfcAdapter.getDefaultAdapter(context) == null }

    // Dim layer. Swallows touches so the blurred home screen behind cannot be used.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
            // Leave room under the card for the timer line and the notices.
            val reserved = 72.dp
            val cardWidth = minOf(maxWidth * 0.86f, (maxHeight - reserved) * CardAspect)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(modifier = Modifier.width(cardWidth)) {
                    Image(
                        painter = painterResource(R.drawable.personal_nfc_card),
                        contentDescription = stringResource(R.string.personal_card_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cardWidth / CardAspect)
                            .clip(CardShape),
                    )
                    // Inset from the corner so it clears the rounded edge.
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

                // Token timer: a straight line, as wide as the card, that shortens to nothing.
                // Hidden until the server has made the payment, and once a business has claimed it.
                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (ready && !claimed) Color.White.copy(alpha = 0.25f) else Color.Transparent),
                ) {
                    if (ready && !claimed) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(cardWidth * progress.value)
                                .clip(CircleShape)
                                .background(Color.White),
                        )
                    }
                }

                if (claimed) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.personal_card_waiting),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }

                if (!nfcOn) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            if (nfcMissing) R.string.personal_card_nfc_missing else R.string.personal_card_nfc_off,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    if (!nfcMissing) {
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }) {
                            Text(stringResource(R.string.personal_card_nfc_settings), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
