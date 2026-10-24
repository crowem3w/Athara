package social.ahtara.authentication.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
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
import social.ahtara.authentication.ui.theme.AuthenticationTheme









class AuthenticationActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Presented as a modal: slides up over Home.
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.authentication_modal_in, R.anim.authentication_modal_hold)
        setContent {
            AuthenticationTheme {
                val state by viewModel.state.collectAsState()
                LaunchedEffect(state.signedIn) {
                    if (state.signedIn) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
                
                
                BackHandler(enabled = state.mode != AuthMode.SIGN_IN) {
                    viewModel.setMode(if (state.mode == AuthMode.SIGN_UP) AuthMode.SELECT_ACCOUNT else AuthMode.SIGN_IN, null)
                }
                Box(Modifier.fillMaxSize()) {
                when (state.mode) {
                    AuthMode.SIGN_IN -> LoginScreen(
                        state = state,
                        onPassword = viewModel::onPassword,
                        onSubmit = viewModel::submit,
                        onLoginId = viewModel::onLoginId,
                        onForgotPassword = { viewModel.setMode(AuthMode.FORGOT_PASSWORD, null) },
                        onSocial = viewModel::signInWithSocial,
                        onSignUp = { viewModel.setMode(AuthMode.SELECT_ACCOUNT, null) },
                        social = viewModel.loginSocialActions,
                    )
                    
                    AuthMode.SELECT_ACCOUNT, AuthMode.SIGN_UP -> SignUpFlowScreen(
                        state = state,
                        onBack = {
                            viewModel.setMode(
                                if (state.mode == AuthMode.SIGN_UP) AuthMode.SELECT_ACCOUNT else AuthMode.SIGN_IN,
                                null,
                            )
                        },
                        onSelectType = viewModel::selectAccountType,
                        onSignIn = { viewModel.setMode(AuthMode.SIGN_IN, null) },
                        onMode = viewModel::setMode,
                        onName = viewModel::onName,
                        onFirstName = viewModel::onFirstName,
                        birthday = viewModel.birthdayActions,
                        onSurname = viewModel::onSurname,
                        onUsername = viewModel::onUsername,
                        onEmail = viewModel::onEmail,
                        onPassword = viewModel::onPassword,
                        onConfirm = viewModel::onConfirm,
                        onSubmit = viewModel::submit,
                        onSocial = viewModel::signInWithSocial,
                        onToSignUp = { viewModel.setMode(AuthMode.SELECT_ACCOUNT, null) },
                        onToSignIn = { viewModel.setMode(AuthMode.SIGN_IN, null) },
                        onIndustry = viewModel::onIndustry,
                        onPhone = viewModel::onPhone,
                        onRegistrationId = viewModel::onRegistrationId,
                        onMobile = viewModel::onMobile,
                        onCountry = viewModel::onCountry,
                        social = viewModel.socialActions,
                    )
                    
                    AuthMode.FORGOT_PASSWORD -> AuthenticationScreen(
                        state = state,
                        onMode = viewModel::setMode,
                        onName = viewModel::onName,
                        onLoginId = viewModel::onLoginId,
                        onPassword = viewModel::onPassword,
                        onConfirm = viewModel::onConfirm,
                        onSubmit = viewModel::submit,
                        onSocial = viewModel::signInWithSocial,
                        onToSignUp = { viewModel.setMode(AuthMode.SELECT_ACCOUNT, null) },
                        onToSignIn = { viewModel.setMode(AuthMode.SIGN_IN, null) },
                    )
                }
                // Dismiss the modal and go back to Home as a guest. Other steps use their own back arrow.
                if (state.mode == AuthMode.SIGN_IN) {
                    IconButton(
                        onClick = { finish() },
                        modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.authentication_ic_close),
                            contentDescription = stringResource(R.string.authentication_action_close),
                        )
                    }
                }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.authentication_modal_hold, R.anim.authentication_modal_out)
    }
}

@Composable
internal fun AuthenticationScreen(
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
    onBack: (() -> Unit)? = null,
) {
    val mode = state.mode
    if (mode == AuthMode.SIGN_UP && onBack != null) {
        RegistrationScreen(accountType = state.accountType, title = registrationTitle(state), onBack = onBack) {
            AuthenticationFormContent(
                state = state,
                onMode = onMode,
                onName = onName,
                onPassword = onPassword,
                onConfirm = onConfirm,
                onSubmit = onSubmit,
                onSocial = onSocial,
                onToSignUp = onToSignUp,
                onToSignIn = onToSignIn,
                onIndustry = onIndustry,
                onPhone = onPhone,
                onRegistrationId = onRegistrationId,
                onFirstName = onFirstName,
                onSurname = onSurname,
                onUsername = onUsername,
                onEmail = onEmail,
                onLoginId = onLoginId,
                birthday = birthday,
                onMobile = onMobile,
                onCountry = onCountry,
                social = social,
                showTitle = false,
            )
        }
        return
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    
                    top = if (mode == AuthMode.SIGN_UP) 32.dp else 20.dp,
                    bottom = 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (mode == AuthMode.SIGN_UP && onBack != null) {
                SignUpStepHeader(currentStep = STEP_REGISTRATION, onBack = onBack)
                Spacer(Modifier.height(4.dp)) 
            }
            AuthenticationFormContent(
                state = state,
                onMode = onMode,
                onName = onName,
                onPassword = onPassword,
                onConfirm = onConfirm,
                onSubmit = onSubmit,
                onSocial = onSocial,
                onToSignUp = onToSignUp,
                onToSignIn = onToSignIn,
                onIndustry = onIndustry,
                onPhone = onPhone,
                onRegistrationId = onRegistrationId,
                onFirstName = onFirstName,
                onSurname = onSurname,
                onUsername = onUsername,
                onEmail = onEmail,
                onLoginId = onLoginId,
                birthday = birthday,
                onMobile = onMobile,
                onCountry = onCountry,
                social = social,
            )
        }
    }
}


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
internal fun SignUpFlowScreen(
    state: AuthUiState,
    onBack: () -> Unit,
    onSelectType: (AccountType) -> Unit,
    onSignIn: () -> Unit,
    onMode: (AuthMode, Int?) -> Unit,
    onName: (String) -> Unit,
    onPassword: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onSubmit: () -> Unit,
    onSocial: (SocialProvider) -> Unit,
    onToSignUp: () -> Unit,
    onToSignIn: () -> Unit,
    onIndustry: (String) -> Unit,
    onPhone: (String) -> Unit,
    onRegistrationId: (String) -> Unit,
    onFirstName: (String) -> Unit = {},
    onSurname: (String) -> Unit = {},
    onUsername: (String) -> Unit = {},
    onEmail: (String) -> Unit = {},
    onLoginId: (String) -> Unit = {},
    birthday: BirthdayActions = BirthdayActions.None,
    onMobile: (String) -> Unit,
    onCountry: (Country) -> Unit,
    social: SocialActions = SocialActions.None,
) {
    val registering = state.mode == AuthMode.SIGN_UP

    
    
    var lastFormState by remember { mutableStateOf(state.copy(mode = AuthMode.SIGN_UP)) }
    if (registering) SideEffect { lastFormState = state }
    val formState = if (registering) state else lastFormState

    val background by animateColorAsState(
        targetValue = if (registering) {
            registrationTint(colorResource(formState.accountType.accentColor))
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(150),
        label = "signUpBackground",
    )

    Surface(modifier = Modifier.fillMaxSize(), color = background) {
        Column(modifier = Modifier.statusBarsPadding().imePadding()) {
            SignUpStepHeader(
                currentStep = if (registering) STEP_REGISTRATION else STEP_ACCOUNT_TYPE,
                onBack = onBack,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp),
            )
            AnimatedContent(
                targetState = registering,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "signUpStep",
            ) { showForm ->
                if (showForm) {
                    AuthenticationTheme(formState.accountType) {
                        RegistrationBody(accountType = formState.accountType, title = registrationTitle(formState)) {
                            AuthenticationFormContent(
                                state = formState,
                                onMode = onMode,
                                onName = onName,
                                onPassword = onPassword,
                                onConfirm = onConfirm,
                                onSubmit = onSubmit,
                                onSocial = onSocial,
                                onToSignUp = onToSignUp,
                                onToSignIn = onToSignIn,
                                onIndustry = onIndustry,
                                onPhone = onPhone,
                                onRegistrationId = onRegistrationId,
                                onFirstName = onFirstName,
                                onSurname = onSurname,
                                onUsername = onUsername,
                                onEmail = onEmail,
                                onLoginId = onLoginId,
                                birthday = birthday,
                                onMobile = onMobile,
                                onCountry = onCountry,
                                social = social,
                                showTitle = false,
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp),
                    ) {
                        AccountTypeScreen(onSelect = onSelectType, onSignIn = onSignIn)
                    }
                }
            }
        }
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