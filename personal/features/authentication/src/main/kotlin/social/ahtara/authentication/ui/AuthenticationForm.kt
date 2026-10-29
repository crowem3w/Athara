package social.ahtara.authentication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.R
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.Country
import social.ahtara.authentication.domain.SocialProvider

@Composable
internal fun ColumnScope.AuthenticationFormContent(
    state: AuthUiState,
    onMode: (AuthMode, Int?) -> Unit,
    onName: (String) -> Unit,
    onPassword: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onSubmit: () -> Unit,
    onSocial: (SocialProvider) -> Unit,
    onToSignUp: () -> Unit,
    onToSignIn: () -> Unit,
    onIndustry: (String) -> Unit = {},
    onPhone: (String) -> Unit = {},
    onRegistrationId: (String) -> Unit = {},
    onFirstName: (String) -> Unit = {},
    onSurname: (String) -> Unit = {},
    onUsername: (String) -> Unit = {},
    onEmail: (String) -> Unit = {},
    onLoginId: (String) -> Unit = {},
    birthday: BirthdayActions = BirthdayActions.None,
    onMobile: (String) -> Unit = {},
    onCountry: (Country) -> Unit = {},
    social: SocialActions = SocialActions.None,
    
    showTitle: Boolean = true,
) {
    val mode = state.mode
    if (mode == AuthMode.SIGN_UP && state.socialAccount != null) {
        SocialCompleteContent(state = state, onMobile = onMobile, onCountry = onCountry, actions = social, showTitle = showTitle)
        return
    }
    if (showTitle) Text(
        text = stringResource(
            when (mode) {
                AuthMode.SIGN_UP -> state.accountType.signUpTitle
                AuthMode.FORGOT_PASSWORD -> R.string.authentication_title_reset
                AuthMode.SIGN_IN, AuthMode.SELECT_ACCOUNT -> R.string.authentication_title_sign_in
            }
        ),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
    )
    if (mode == AuthMode.FORGOT_PASSWORD) {
        Text(
            text = stringResource(R.string.authentication_subtitle_reset),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    state.info?.let {
        Text(stringResource(it), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(8.dp)) 

    when {
        
        state.isPersonalSignUp -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AuthField(
                    value = state.firstName, onChange = onFirstName,
                    label = R.string.authentication_label_first_name, error = state.nameError,
                    keyboardType = KeyboardType.Text, imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words,
                    modifier = Modifier.weight(0.9f),
                )
                AuthField(
                    value = state.surname, onChange = onSurname,
                    label = R.string.authentication_label_surname, error = null,
                    keyboardType = KeyboardType.Text, imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words,
                    modifier = Modifier.weight(1.1f),
                )
            }
            AuthField(
                value = state.username, onChange = onUsername,
                label = R.string.authentication_label_username, error = state.usernameError,
                keyboardType = KeyboardType.Text, imeAction = ImeAction.Next,
            )
            BirthdayInput(
                month = state.birthMonth, day = state.birthDay, year = state.birthYear,
                error = state.birthdayError, actions = birthday,
            )
            AuthField(
                value = state.email, onChange = onEmail,
                label = R.string.authentication_label_email_optional, error = state.emailError,
                keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
            )
            PhMobileInput(
                value = state.mobile, onChange = onMobile,
                error = state.identifierError, imeAction = ImeAction.Next,
            )
        }
        mode == AuthMode.SIGN_UP -> {
            AuthField(
                value = state.name, onChange = onName,
                label = state.accountType.nameLabel, error = state.nameError,
                keyboardType = KeyboardType.Text, imeAction = ImeAction.Next,
            )
            IdentifierInput(
                state = state,
                onMobile = onMobile,
                onCountry = onCountry,
                imeAction = ImeAction.Next,
            )
        }
        
        else -> LoginIdInput(
            value = state.loginId,
            onChange = onLoginId,
            error = state.identifierError,
            imeAction = ImeAction.Next,
        )
    }
    if (mode == AuthMode.SIGN_UP && state.accountType.hasDetails) {
        state.accountType.industryLabel?.let { label ->
            AuthField(
                value = state.industry, onChange = onIndustry,
                label = label, error = state.industryError,
                keyboardType = KeyboardType.Text, imeAction = ImeAction.Next,
            )
        }
        AuthField(
            value = state.phone, onChange = onPhone,
            label = R.string.authentication_label_phone, error = state.phoneError,
            keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next,
        )
        state.accountType.registrationLabel?.let { label ->
            AuthField(
                value = state.registrationId, onChange = onRegistrationId,
                label = label, error = null,
                keyboardType = KeyboardType.Text, imeAction = ImeAction.Next,
            )
        }
    }
    AuthField(
        value = state.password, onChange = onPassword,
        label = if (mode == AuthMode.FORGOT_PASSWORD) R.string.authentication_label_new_password
        else R.string.authentication_label_password,
        error = state.passwordError, isPassword = true,
        keyboardType = KeyboardType.Password,
        imeAction = if (mode == AuthMode.SIGN_IN) ImeAction.Done else ImeAction.Next,
        onDone = onSubmit,
    )
    if (mode != AuthMode.SIGN_IN) {
        AuthField(
            value = state.confirmPassword, onChange = onConfirm,
            label = R.string.authentication_label_confirm_password, error = state.confirmError,
            isPassword = true, keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done, onDone = onSubmit,
        )
    }

    state.formError?.let {
        Text(
            text = state.formErrorType
                ?.let { type -> stringResource(it, stringResource(type.title)) }
                ?: stringResource(it),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (mode == AuthMode.SIGN_IN) {
        TextButton(
            onClick = { onMode(AuthMode.FORGOT_PASSWORD, null) },
            modifier = Modifier.align(Alignment.End),
        ) { Text(stringResource(R.string.authentication_action_forgot)) }
    }

    Button(
        onClick = onSubmit,
        enabled = !state.isLoading,
        modifier = Modifier.fillMaxWidth().height(ButtonHeight),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.height(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(
                stringResource(
                    when (mode) {
                        AuthMode.SIGN_UP -> R.string.authentication_action_sign_up
                        AuthMode.FORGOT_PASSWORD -> R.string.authentication_action_reset
                        AuthMode.SIGN_IN, AuthMode.SELECT_ACCOUNT -> R.string.authentication_action_sign_in
                    }
                )
            )
        }
    }

    
    if (mode == AuthMode.SIGN_UP && state.accountType == AccountType.PERSONAL) {
        SocialSignUpSection(isLoading = state.isLoading, actions = social)
    }

    if (mode == AuthMode.SIGN_IN) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(Modifier.weight(1f))
            Text(stringResource(R.string.authentication_or), style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(Modifier.weight(1f))
        }
        OutlinedButton(
            onClick = { onSocial(SocialProvider.GOOGLE) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(ButtonHeight),
        ) { Text(stringResource(R.string.authentication_social_google)) }
        OutlinedButton(
            onClick = { onSocial(SocialProvider.APPLE) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(ButtonHeight),
        ) { Text(stringResource(R.string.authentication_social_apple)) }
        Text(
            text = stringResource(R.string.authentication_social_demo_note),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    TextButton(
        onClick = {
            when (mode) {
                AuthMode.SIGN_IN, AuthMode.SELECT_ACCOUNT -> onToSignUp()
                AuthMode.SIGN_UP -> onToSignIn()
                AuthMode.FORGOT_PASSWORD -> onMode(AuthMode.SIGN_IN, null)
            }
        },
        modifier = Modifier.align(Alignment.CenterHorizontally),
    ) {
        Text(
            stringResource(
                when (mode) {
                    AuthMode.SIGN_IN, AuthMode.SELECT_ACCOUNT -> R.string.authentication_action_to_sign_up
                    AuthMode.SIGN_UP -> R.string.authentication_action_to_sign_in
                    AuthMode.FORGOT_PASSWORD -> R.string.authentication_action_back_to_sign_in
                }
            )
        )
    }
}







@Composable
private fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    label: Int,
    error: Int?,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    isPassword: Boolean = false,
    onDone: () -> Unit = {},
    modifier: Modifier = Modifier,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    var visible by remember { mutableStateOf(false) }
    CompactTextField(
        modifier = modifier,
        value = value,
        onValueChange = onChange,
        placeholder = stringResource(label),
        error = error,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }),
        trailingIcon = if (isPassword) {
            {
                TextButton(onClick = { visible = !visible }) {
                    Text(stringResource(if (visible) R.string.authentication_hide else R.string.authentication_show))
                }
            }
        } else null,
        shape = RoundedCornerShape(12.dp),
    )
}
