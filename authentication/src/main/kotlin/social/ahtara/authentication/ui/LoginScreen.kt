package social.ahtara.authentication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.R
import social.ahtara.authentication.domain.SocialProvider
import social.ahtara.authentication.ui.theme.AuthenticationTheme






@Composable
internal fun LoginScreen(
    state: AuthUiState,
    onPassword: (String) -> Unit,
    onSubmit: () -> Unit,
    onLoginId: (String) -> Unit,
    onForgotPassword: () -> Unit,
    onSocial: (SocialProvider) -> Unit,
    onSignUp: () -> Unit,
    
    social: SocialActions = SocialActions.None,
) {
    val google = rememberGoogleSignIn(social)
    val muted = colorResource(R.color.authentication_muted)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            val screenHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    
                    
                    .heightIn(min = screenHeight)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                
                
                
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AuthLogo(width = 120.dp)
                        Spacer(Modifier.height(12.dp))
                        LoginRoadStrip()
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.authentication_title_sign_in),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.authentication_login_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(28.dp)) 

                state.info?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }

                LoginIdInput(
                    value = state.loginId,
                    onChange = onLoginId,
                    error = state.identifierError,
                    imeAction = ImeAction.Next,
                    pill = true,
                )
                Spacer(Modifier.height(10.dp))
                LoginField(
                    value = state.password,
                    onChange = onPassword,
                    placeholder = R.string.authentication_label_password,
                    leadingIcon = Icons.Outlined.Lock,
                    error = state.passwordError,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    isPassword = true,
                    onDone = onSubmit,
                )

                state.formError?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp, start = 16.dp),
                    )
                }

                TextButton(
                    onClick = onForgotPassword,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = stringResource(R.string.authentication_action_forgot),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(2.dp))

                Button(
                    onClick = onSubmit,
                    enabled = !state.isLoading,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(ButtonHeight),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.authentication_action_log_in),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(Modifier.weight(1f), color = colorResource(R.color.authentication_field_border))
                    Text(
                        text = stringResource(R.string.authentication_or_continue_with),
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                    )
                    HorizontalDivider(Modifier.weight(1f), color = colorResource(R.color.authentication_field_border))
                }
                Spacer(Modifier.height(14.dp))

                SocialButton(
                    text = R.string.authentication_social_google,
                    icon = R.drawable.authentication_ic_google,
                    enabled = !state.isLoading && !google.picking,
                    onClick = google.start,
                )
                Spacer(Modifier.height(10.dp))
                SocialButton(
                    text = R.string.authentication_social_apple,
                    icon = R.drawable.authentication_ic_apple,
                    enabled = !state.isLoading && !google.picking,
                    onClick = { onSocial(SocialProvider.APPLE) },
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.authentication_login_no_account),
                        style = MaterialTheme.typography.bodyLarge,
                        color = muted,
                    )
                    TextButton(onClick = onSignUp) {
                        Text(
                            text = stringResource(R.string.authentication_action_sign_up_short),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: Int,
    leadingIcon: ImageVector,
    error: Int?,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    isPassword: Boolean = false,
    onDone: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    val muted = colorResource(R.color.authentication_muted)
    val container = colorResource(R.color.authentication_field_background)
    CompactTextField(
        value = value,
        onValueChange = onChange,
        placeholder = stringResource(placeholder),
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { visible = !visible }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        painter = painterResource(
                            if (visible) R.drawable.authentication_ic_visibility_off
                            else R.drawable.authentication_ic_visibility
                        ),
                        contentDescription = stringResource(
                            if (visible) R.string.authentication_hide else R.string.authentication_show
                        ),
                    )
                }
            }
        } else null,
        error = error,
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            errorContainerColor = container,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = colorResource(R.color.authentication_field_border),
            focusedLeadingIconColor = muted,
            unfocusedLeadingIconColor = muted,
            focusedTrailingIconColor = muted,
            unfocusedTrailingIconColor = muted,
            focusedPlaceholderColor = muted,
            unfocusedPlaceholderColor = muted,
        ),
    )
}

@Composable
private fun SocialButton(
    text: Int,
    icon: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
        modifier = Modifier.fillMaxWidth().height(ButtonHeight),
    ) {
        
        Icon(painterResource(icon), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun LoginScreenPreview() {
    AuthenticationTheme {
        LoginScreen(
            state = AuthUiState(mode = AuthMode.SIGN_IN),
            onPassword = {}, onSubmit = {}, onLoginId = {},
            onForgotPassword = {}, onSocial = {}, onSignUp = {},
        )
    }
}