package org.example.authentication.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.authentication.Authentication
import org.example.authentication.R
import org.example.identityapi.AccountDetails
import org.example.identityapi.AccountType
import org.example.identityapi.AuthError
import org.example.identityapi.AuthRepository
import org.example.identityapi.AuthResult
import org.example.authentication.domain.Countries
import org.example.authentication.domain.Country
import org.example.identityapi.SocialAccount
import org.example.identityapi.SocialProvider
import org.example.authentication.domain.Validators
import org.example.authentication.domain.Validators.Problem

internal enum class AuthMode { SELECT_ACCOUNT, SIGN_IN, SIGN_UP, FORGOT_PASSWORD }

internal data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val accountType: AccountType = AccountType.PERSONAL,
    /** Business / organization name. Personal accounts use [firstName] / [surname] instead. */
    val name: String = "",
    val firstName: String = "",
    val surname: String = "",
    val username: String = "",
    val email: String = "",
    /** Personal birthday, as three separate digit fields. */
    val birthMonth: String = "",
    val birthDay: String = "",
    val birthYear: String = "",
    /** Sign-in / password reset: what the user typed, a username, email or mobile number. */
    val loginId: String = "",
    val country: Country = Countries.DEFAULT,
    /** Mobile number as typed, without the country code. */
    val mobile: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    /** Business / organization only. */
    val industry: String = "",
    val phone: String = "",
    val registrationId: String = "",
    @param:StringRes val nameError: Int? = null,
    @param:StringRes val usernameError: Int? = null,
    @param:StringRes val emailError: Int? = null,
    @param:StringRes val birthdayError: Int? = null,
    /** Error for the mobile-number identifier. */
    @param:StringRes val identifierError: Int? = null,
    @param:StringRes val passwordError: Int? = null,
    @param:StringRes val confirmError: Int? = null,
    @param:StringRes val industryError: Int? = null,
    @param:StringRes val phoneError: Int? = null,
    /** Error from the repository (wrong password, number taken, ...). */
    @param:StringRes val formError: Int? = null,
    /** Set with IDENTIFIER_TAKEN: the account type the number is already registered under. */
    val formErrorType: AccountType? = null,
    /** Success notice (e.g. password reset). */
    @param:StringRes val info: Int? = null,
    val isLoading: Boolean = false,
    val signedIn: Boolean = false,
    /** Set after Google / Apple succeeded for a new user: the form then only asks for a mobile number. */
    val socialAccount: SocialAccount? = null,
) {
    /** What the repository sees: an E.164 number such as +639171234567. */
    val identifier: String
        // Personal sign-up has a fixed +63 prefix instead of a country picker.
        get() = Validators.toE164(if (isPersonalSignUp) Countries.DEFAULT else country, mobile)

    /** Personal sign-up has its own fields; its mobile number is optional. */
    val isPersonalSignUp: Boolean
        get() = mode == AuthMode.SIGN_UP && accountType == AccountType.PERSONAL && socialAccount == null

    val displayName: String
        get() = if (accountType == AccountType.PERSONAL) {
            listOf(firstName, surname).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
        } else name.trim()

    /** Keeps the identifier fields when switching screens. */
    fun carryIdentifier(mode: AuthMode, info: Int? = null) = AuthUiState(
        mode = mode, accountType = accountType,
        country = country, mobile = mobile, loginId = loginId, info = info,
    )
}

internal class AuthViewModel(
    private val repository: AuthRepository,
    initial: AuthUiState = AuthUiState(),
) : ViewModel() {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun setMode(mode: AuthMode, info: Int? = null) = _state.update {
        // Keep the mobile number when switching screens; clear everything else.
        it.carryIdentifier(mode, info)
    }

    /** Account type picked on the selection screen: opens that type's sign-up form. */
    fun selectAccountType(type: AccountType) = _state.update {
        it.carryIdentifier(AuthMode.SIGN_UP).copy(
            accountType = type,
            mobile = if (type == AccountType.PERSONAL) Validators.cleanPhMobile(it.mobile) else it.mobile,
        )
    }

    fun onName(v: String) = _state.update { it.copy(name = v, nameError = null, formError = null) }
    fun onFirstName(v: String) = _state.update { it.copy(firstName = v, nameError = null, formError = null) }
    fun onSurname(v: String) = _state.update { it.copy(surname = v, formError = null) }
    fun onUsername(v: String) = _state.update {
        it.copy(username = v.filterNot { c -> c.isWhitespace() }, usernameError = null, formError = null)
    }
    fun onEmail(v: String) = _state.update {
        it.copy(email = v.filterNot { c -> c.isWhitespace() }, emailError = null, formError = null)
    }
    fun onLoginId(v: String) = _state.update { it.copy(loginId = v, identifierError = null, formError = null, info = null) }
    fun onCountry(c: Country) = _state.update { it.copy(country = c, identifierError = null, formError = null, info = null) }
    fun onMobile(v: String) = _state.update {
        // Personal: digits only (the field shows them as 9XX XXX XXXX); other forms keep what was typed.
        val cleaned = if (it.isPersonalSignUp) Validators.cleanPhMobile(v) else v.filter { c -> c.isDigit() || c in "+ -()" }
        it.copy(mobile = cleaned, identifierError = null, formError = null, info = null)
    }
    fun onBirthMonth(v: String) = _state.update {
        it.copy(birthMonth = v.filter(Char::isDigit).take(2), birthdayError = null, formError = null)
    }
    fun onBirthDay(v: String) = _state.update {
        it.copy(birthDay = v.filter(Char::isDigit).take(2), birthdayError = null, formError = null)
    }
    fun onBirthYear(v: String) = _state.update {
        it.copy(birthYear = v.filter(Char::isDigit).take(4), birthdayError = null, formError = null)
    }
    /** Date picker result: fills the three fields. */
    fun onBirthDate(month: Int, day: Int, year: Int) = _state.update {
        it.copy(
            birthMonth = "%02d".format(month), birthDay = "%02d".format(day), birthYear = "%04d".format(year),
            birthdayError = null, formError = null,
        )
    }

    /** What the birthday row (three text fields + date picker) calls. */
    val birthdayActions = BirthdayActions(
        onMonth = ::onBirthMonth,
        onDay = ::onBirthDay,
        onYear = ::onBirthYear,
        onDate = ::onBirthDate,
    )
    fun onPassword(v: String) = _state.update { it.copy(password = v, passwordError = null, formError = null) }
    fun onIndustry(v: String) = _state.update { it.copy(industry = v, industryError = null, formError = null) }
    fun onPhone(v: String) = _state.update { it.copy(phone = v, phoneError = null, formError = null) }
    fun onRegistrationId(v: String) = _state.update { it.copy(registrationId = v, formError = null) }
    fun onConfirm(v: String) = _state.update { it.copy(confirmPassword = v, confirmError = null, formError = null) }

    fun submit() {
        val s = _state.value
        if (s.mode == AuthMode.SELECT_ACCOUNT || s.isLoading || !validate(s)) return
        _state.update { it.copy(isLoading = true, formError = null, info = null) }
        viewModelScope.launch {
            val result = when (s.mode) {
                AuthMode.SIGN_IN -> repository.signIn(Validators.loginKey(s.country, s.loginId), s.password)
                AuthMode.SIGN_UP ->
                    if (s.isPersonalSignUp) repository.signUp(
                        name = s.displayName,
                        identifier = if (s.mobile.isBlank()) null else s.identifier,
                        password = s.password,
                        accountType = s.accountType,
                        username = s.username.trim(),
                        email = s.email.trim().ifBlank { null },
                        birthday = Validators.birthdayIso(s.birthMonth, s.birthDay, s.birthYear),
                    ) else repository.signUp(
                        s.displayName, s.identifier, s.password, s.accountType,
                        if (s.accountType.hasDetails) AccountDetails(s.industry, s.phone, s.registrationId) else AccountDetails(),
                    )
                AuthMode.FORGOT_PASSWORD -> repository.resetPassword(Validators.loginKey(s.country, s.loginId), s.password)
                AuthMode.SELECT_ACCOUNT -> return@launch
            }
            handle(result, s.mode)
        }
    }

    /** What the registration form's Google / Apple buttons and the "mobile number" step call. */
    val socialActions = SocialActions(
        onAccount = ::onSocialAccount,
        onApple = ::continueWithApple,
        onError = ::onSocialError,
        onCancel = ::cancelSocial,
        onSubmit = ::submitSocial,
    )

    /**
     * A social account was picked. If it is already linked to a user, sign in; otherwise keep it
     * in the state so the form asks for a mobile number to finish creating the account.
     */
    fun onSocialAccount(account: SocialAccount) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, formError = null, info = null) }
        viewModelScope.launch {
            val result = repository.continueWithSocial(account)
            if (result is AuthResult.Failure && result.error == AuthError.ACCOUNT_NOT_FOUND) {
                _state.update { it.copy(isLoading = false, socialAccount = account, identifierError = null) }
            } else {
                handle(result, AuthMode.SIGN_UP)
            }
        }
    }

    /** Login screen's Google / Apple callbacks: Google signs in the linked user, nothing else. */
    val loginSocialActions = SocialActions(
        onAccount = ::signInWithSocialAccount,
        onError = ::onSocialError,
    )

    /** Signs in the user linked to a Google account; a Google account that never registered gets a message. */
    fun signInWithSocialAccount(account: SocialAccount) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, formError = null, info = null) }
        viewModelScope.launch {
            val result = repository.continueWithSocial(account)
            if (result is AuthResult.Failure && result.error == AuthError.ACCOUNT_NOT_FOUND) {
                _state.update {
                    it.copy(isLoading = false, formError = R.string.authentication_error_social_not_registered, formErrorType = null)
                }
            } else {
                handle(result, AuthMode.SIGN_IN)
            }
        }
    }

    /** Apple sign-in is not implemented: no ID token, so the backend refuses it. */
    fun continueWithApple() = onSocialAccount(
        SocialAccount(SocialProvider.APPLE, subject = "demo.apple", email = null, displayName = "Demo Apple User")
    )

    fun onSocialError(@StringRes message: Int?) =
        _state.update { it.copy(isLoading = false, formError = message, formErrorType = null) }

    /** "Use a different account": back to the normal registration form. */
    fun cancelSocial() = _state.update {
        it.copy(socialAccount = null, mobile = "", identifierError = null, formError = null, formErrorType = null)
    }

    /** Finishes social sign-up with the mobile number the user typed. */
    fun submitSocial() {
        val s = _state.value
        val account = s.socialAccount ?: return
        if (s.isLoading) return
        val problem = Validators.mobile(s.country, s.mobile)
        if (problem != null) {
            _state.update {
                it.copy(
                    identifierError = if (problem == Problem.REQUIRED) R.string.authentication_error_mobile_required
                    else R.string.authentication_error_mobile_invalid,
                )
            }
            return
        }
        _state.update { it.copy(isLoading = true, formError = null, info = null) }
        viewModelScope.launch {
            handle(repository.signUpWithSocial(account, s.identifier, s.accountType), AuthMode.SIGN_UP)
        }
    }

    fun signInWithSocial(provider: SocialProvider) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, formError = null, info = null) }
        viewModelScope.launch { handle(repository.signInWithSocial(provider), AuthMode.SIGN_IN) }
    }

    private fun handle(result: AuthResult, mode: AuthMode) {
        when (result) {
            is AuthResult.Success ->
                if (mode == AuthMode.FORGOT_PASSWORD) {
                    _state.update { it.carryIdentifier(AuthMode.SIGN_IN, R.string.authentication_message_password_reset) }
                } else {
                    _state.update { it.copy(isLoading = false, signedIn = true) }
                }
            is AuthResult.Failure ->
                _state.update {
                    when (result.error) {
                        AuthError.USERNAME_TAKEN -> return@update it.copy(
                            isLoading = false, usernameError = R.string.authentication_error_username_taken,
                        )
                        AuthError.EMAIL_TAKEN -> return@update it.copy(
                            isLoading = false, emailError = R.string.authentication_error_email_taken,
                        )
                        else -> Unit
                    }
                    it.copy(
                        isLoading = false,
                        formError = if (result.error == AuthError.IDENTIFIER_TAKEN && result.existingType != null)
                            R.string.authentication_error_identifier_taken_type else result.error.toMessage(),
                        formErrorType = result.existingType,
                    )
                }
        }
    }

    private fun validate(s: AuthUiState): Boolean {
        val personal = s.isPersonalSignUp
        val nameErr = when {
            s.mode != AuthMode.SIGN_UP -> null
            personal -> Validators.name(s.firstName)?.let { R.string.authentication_error_first_name_required }
            else -> Validators.name(s.name)?.let { R.string.authentication_error_name_required }
        }
        val usernameErr = if (personal) Validators.username(s.username)?.let {
            if (it == Problem.REQUIRED) R.string.authentication_error_username_required
            else R.string.authentication_error_username_invalid
        } else null
        val birthdayErr = if (personal) Validators.birthday(s.birthMonth, s.birthDay, s.birthYear)?.let {
            when (it) {
                Problem.REQUIRED -> R.string.authentication_error_birthday_required
                Problem.UNDERAGE -> R.string.authentication_error_birthday_underage
                else -> R.string.authentication_error_birthday_invalid
            }
        } else null
        // Email and mobile number are optional on Personal sign-up: only checked when filled in.
        val emailErr = if (personal && s.email.isNotBlank()) {
            Validators.email(s.email)?.let { R.string.authentication_error_email_invalid }
        } else null
        val identifierErr = when {
            s.mode != AuthMode.SIGN_UP -> if (s.loginId.isBlank()) R.string.authentication_error_login_required else null
            personal && s.mobile.isBlank() -> null
            personal -> Validators.mobile(Countries.DEFAULT, s.mobile)?.let { R.string.authentication_error_mobile_invalid_ph }
            else -> Validators.mobile(s.country, s.mobile)?.let {
                if (it == Problem.REQUIRED) R.string.authentication_error_mobile_required
                else R.string.authentication_error_mobile_invalid
            }
        }
        // Signing in only needs a non-empty password; creating/resetting enforces strength.
        val passErr = when (s.mode) {
            AuthMode.SIGN_IN -> Validators.existingPassword(s.password)
            else -> Validators.newPassword(s.password)
        }?.let {
            if (it == Problem.REQUIRED) R.string.authentication_error_password_required
            else R.string.authentication_error_password_weak
        }
        val confirmErr = if (s.mode != AuthMode.SIGN_IN && passErr == null && s.password != s.confirmPassword)
            R.string.authentication_error_password_mismatch else null

        val details = s.mode == AuthMode.SIGN_UP && s.accountType.hasDetails
        val industryErr = if (details) Validators.required(s.industry)?.let { R.string.authentication_error_industry_required } else null
        val phoneErr = if (details) Validators.phone(s.phone)?.let {
            if (it == Problem.REQUIRED) R.string.authentication_error_phone_required
            else R.string.authentication_error_phone_invalid
        } else null

        _state.update {
            it.copy(
                nameError = nameErr, usernameError = usernameErr, emailError = emailErr, birthdayError = birthdayErr,
                identifierError = identifierErr, passwordError = passErr, confirmError = confirmErr,
                industryError = industryErr, phoneError = phoneErr,
            )
        }
        return nameErr == null && usernameErr == null && birthdayErr == null && emailErr == null && identifierErr == null && passErr == null && confirmErr == null &&
            industryErr == null && phoneErr == null
    }

    private fun AuthError.toMessage(): Int = when (this) {
        AuthError.INVALID_CREDENTIALS -> R.string.authentication_error_invalid_credentials
        AuthError.IDENTIFIER_TAKEN -> R.string.authentication_error_identifier_taken
        AuthError.USERNAME_TAKEN -> R.string.authentication_error_username_taken
        AuthError.EMAIL_TAKEN -> R.string.authentication_error_email_taken
        AuthError.ACCOUNT_NOT_FOUND -> R.string.authentication_error_account_not_found
        AuthError.NETWORK -> R.string.authentication_error_network
        AuthError.VERIFICATION_REQUIRED -> R.string.authentication_error_verification_required
        AuthError.UNKNOWN -> R.string.authentication_error_unknown
    }

    companion object {
        /** Used by AuthenticationActivity: starts on the login screen. */
        val Factory = factory(AuthMode.SIGN_IN, AccountType.PERSONAL)

        /** Used by the :personal / :business / :organization sign-up forms. */
        fun factory(initialMode: AuthMode, accountType: AccountType) = viewModelFactory {
            initializer {
                AuthViewModel(
                    Authentication.repository(this[APPLICATION_KEY]!!),
                    AuthUiState(mode = initialMode, accountType = accountType),
                )
            }
        }
    }
}
