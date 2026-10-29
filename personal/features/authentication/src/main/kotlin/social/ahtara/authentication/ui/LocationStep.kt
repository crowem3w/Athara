package social.ahtara.authentication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import social.ahtara.authentication.R












@Composable
fun LocationStepScreen(
    onBack: () -> Unit,
    preHome: Boolean = false,
    accountType: social.ahtara.authentication.domain.AccountType = social.ahtara.authentication.domain.AccountType.PERSONAL,
    map: @Composable BoxScope.(bottomInset: Dp) -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val panelHeight = maxHeight * PanelShare
            Box(modifier = Modifier.fillMaxSize()) { map(panelHeight) }

            SignUpStepHeader(
                currentStep = if (preHome) STEP_LOCATION else STEP_SETUP,
                onBack = onBack,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp),
            )

            RegistrationPanel(
                title = when (accountType) {
                    social.ahtara.authentication.domain.AccountType.PERSONAL -> R.string.authentication_title_location
                    social.ahtara.authentication.domain.AccountType.BUSINESS -> R.string.authentication_title_location_business
                    social.ahtara.authentication.domain.AccountType.ORGANIZATION -> R.string.authentication_title_location_organization
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .imePadding()
                    .height(panelHeight),
                footer = footer,
                content = content,
            )
        }
    }
}


private const val PanelShare = 0.46f


@Composable
fun LocationSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompactTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
}


/** Free-text address line (Business accounts), shown under the map search. */
@Composable
fun LocationAddressField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    CompactTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    )
}


@Composable
fun StepPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(ButtonHeight),
    ) { Text(text) }
}