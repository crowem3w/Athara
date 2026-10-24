package social.ahtara.authentication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import social.ahtara.authentication.R
import social.ahtara.authentication.domain.AccountType


private const val IllustrationWeight = 0.6f
private const val PanelWeight = 1.4f

private val PanelShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)


private val PanelSideInset = 8.dp


@StringRes
internal fun registrationTitle(state: AuthUiState): Int =
    if (state.socialAccount != null) R.string.authentication_social_complete_title else state.accountType.signUpTitle


internal fun registrationTint(accent: Color): Color = accent.copy(alpha = 0.10f).compositeOver(Color.White)









@Composable
internal fun RegistrationBody(
    accountType: AccountType,
    @StringRes title: Int,
    modifier: Modifier = Modifier.fillMaxSize(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = !imeOpen,
            enter = fadeIn(tween(150)) + expandVertically(tween(200)),
            exit = fadeOut(tween(100)) + shrinkVertically(tween(200)),
            modifier = Modifier.weight(IllustrationWeight),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(accountType.illustration),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        RegistrationPanel(
            title = title,
            modifier = Modifier.weight(PanelWeight).fillMaxWidth().padding(horizontal = PanelSideInset),
            content = content,
        )
    }
}





@Composable
internal fun RegistrationPanel(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = PanelShape,
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 16.dp,
    ) {
        CompositionLocalProvider(LocalSoftFields provides true) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 14.dp),
                )
                
                
                val glow = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .zIndex(1f)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(50),
                            clip = false,
                            ambientColor = glow,
                            spotColor = glow,
                        )
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(50)),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .then(if (footer == null) Modifier.navigationBarsPadding() else Modifier)
                        
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content,
                )
                if (footer != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = footer,
                    )
                }
            }
        }
    }
}





@Composable
internal fun RegistrationScreen(
    accountType: AccountType,
    @StringRes title: Int,
    onBack: () -> Unit,
    currentStep: Int = STEP_REGISTRATION,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = registrationTint(colorResource(accountType.accentColor)),
    ) {
        Column(modifier = Modifier.statusBarsPadding().imePadding()) {
            SignUpStepHeader(
                currentStep = currentStep,
                onBack = onBack,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp),
            )
            RegistrationBody(
                accountType = accountType,
                title = title,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                content = content,
            )
        }
    }
}