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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import soc.athara.app.modules.personal.R
import soc.athara.app.modules.personal.nfc.PaymentTokenHost
import soc.athara.app.modules.personal.nfc.TokenLifetimeSeconds

/** Width / height of `personal_nfc_card` (941 x 1672), so the card keeps its portrait shape on any screen. */
private const val CardAspect = 941f / 1672f

private val CardShape = RoundedCornerShape(28.dp)

/**
 * The receiver's digital card, shown over the (blurred) home screen after Confirm.
 *
 * While it is open the phone serves a payment token over NFC ([PaymentTokenHost]). The line under the card
 * shrinks over [TokenLifetimeSeconds] seconds, then the token is replaced and the line resets, so a token
 * that is read or photographed becomes useless quickly. When a reader receives the token, the phone gives a
 * confirm haptic and [onScanned] runs. [onClose] runs from the X button or the back gesture.
 */
@Composable
internal fun NfcCard(onClose: () -> Unit, onScanned: () -> Unit) {
    val view = LocalView.current
    val context = LocalContext.current
    val latestOnScanned by rememberUpdatedState(onScanned)

    BackHandler(onBack = onClose)

    // Serve a token only while the card is on screen.
    DisposableEffect(Unit) {
        PaymentTokenHost.start()
        onDispose { PaymentTokenHost.stop() }
    }

    // Shrinking line; when it runs out, rotate the token and start again.
    val progress = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            progress.snapTo(1f)
            progress.animateTo(0f, tween(durationMillis = TokenLifetimeSeconds * 1000, easing = LinearEasing))
            PaymentTokenHost.rotate()
        }
    }

    // A reader got the token: confirm haptic, then tell the screen.
    LaunchedEffect(Unit) {
        PaymentTokenHost.delivered.collect {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            latestOnScanned()
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
            // Leave room under the card for the timer line and the NFC notice.
            val reserved = 72.dp
            val cardWidth = minOf(maxWidth * 0.86f, (maxHeight - reserved) * CardAspect)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
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
                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(cardWidth * progress.value)
                            .clip(CircleShape)
                            .background(Color.White),
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
