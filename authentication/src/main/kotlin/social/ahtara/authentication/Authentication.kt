package social.ahtara.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import social.ahtara.authentication.data.LocalAuthRepository
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.AuthRepository
import social.ahtara.authentication.domain.NoVerification
import social.ahtara.authentication.domain.PersonalSetup
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.PhoneVerifier
import social.ahtara.authentication.ui.AuthenticationActivity








object Authentication {
    @Volatile
    private var repository: AuthRepository? = null

    fun repository(context: Context): AuthRepository =
        repository ?: synchronized(this) {
            repository ?: LocalAuthRepository.create(context.applicationContext).also { repository = it }
        }

    
    fun install(repository: AuthRepository) {
        this.repository = repository
    }

    @Volatile
    var phoneVerifier: PhoneVerifier = NoVerification
        private set

    
    fun installPhoneVerifier(verifier: PhoneVerifier) {
        phoneVerifier = verifier
    }

    fun isSignedIn(context: Context): Boolean =
        repository(context).currentUser.value != null

    
    fun isSetupPending(context: Context): Boolean = repository(context).isSetupPending()

    
    fun completeSetup(context: Context) = repository(context).completeSetup()

    
    suspend fun savePersonalSetup(context: Context, setup: PersonalSetup): Boolean =
        repository(context).savePersonalSetup(setup)

    
    suspend fun savePlaces(context: Context, places: List<PlaceSelection>): Boolean =
        repository(context).savePlaces(places)

    
    suspend fun discardNewAccount(context: Context): Boolean = repository(context).discardNewAccount()

    
    suspend fun signOut(context: Context) = repository(context).signOut()

    private const val FLOW_PREFS = "authentication_flow"
    private const val KEY_ACCOUNT_TYPE = "selected_account_type"

    private fun storedAccountType(context: Context): AccountType? =
        context.getSharedPreferences(FLOW_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACCOUNT_TYPE, null)
            ?.let { name -> AccountType.available.firstOrNull { it.name == name } }

    /** Account type used for sign-up: the one picked on the pre-Home step, or the only one offered. */
    fun selectedAccountType(context: Context): AccountType? =
        storedAccountType(context) ?: AccountType.available.singleOrNull()

    /**
     * True when the pre-Home "Account type" step must be shown: more than one type is offered and
     * none was picked yet. While only Personal is offered the step is skipped (nothing is stored,
     * so it still shows once, to everyone, if more types are offered later).
     */
    fun needsAccountTypeStep(context: Context): Boolean =
        AccountType.available.size > 1 && storedAccountType(context) == null

    fun selectAccountType(context: Context, type: AccountType) {
        context.getSharedPreferences(FLOW_PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACCOUNT_TYPE, type.name).apply()
    }
}

/** Which screen of the Login / Registration flow to open first. */
enum class AuthEntry { SIGN_IN, SIGN_UP }


class AuthenticationContract : ActivityResultContract<AuthEntry, Boolean>() {
    override fun createIntent(context: Context, input: AuthEntry): Intent =
        Intent(context, AuthenticationActivity::class.java)
            .putExtra(AuthenticationActivity.EXTRA_ENTRY, input.name)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}