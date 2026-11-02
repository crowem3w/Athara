package org.example.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.example.authentication.data.LocalAuthRepository
import org.example.authentication.domain.AuthRepository
import org.example.authentication.domain.NoVerification
import org.example.authentication.domain.PersonalSetup
import org.example.authentication.domain.PhoneVerifier
import org.example.authentication.ui.AuthenticationActivity

/**
 * Public entry point of the :authentication module.
 *
 * Uses a simple singleton provider (no DI library). To migrate to a real
 * backend, call [install] once with your own [AuthRepository] (e.g. in
 * Application.onCreate); nothing else in the app has to change.
 */
object Authentication {
    @Volatile
    private var repository: AuthRepository? = null

    fun repository(context: Context): AuthRepository =
        repository ?: synchronized(this) {
            repository ?: LocalAuthRepository.create(context.applicationContext).also { repository = it }
        }

    /** Replace the default local repository (call before first use). */
    fun install(repository: AuthRepository) {
        this.repository = repository
    }

    @Volatile
    var phoneVerifier: PhoneVerifier = NoVerification
        private set

    /** Plug in real SMS OTP verification (see [PhoneVerifier]). */
    fun installPhoneVerifier(verifier: PhoneVerifier) {
        phoneVerifier = verifier
    }

    fun isSignedIn(context: Context): Boolean =
        repository(context).currentUser.value != null

    /** True while the signed-in user just created the account and has not finished or skipped the setup step. */
    fun isSetupPending(context: Context): Boolean = repository(context).isSetupPending()

    /** Marks the account setup step as finished or skipped. */
    fun completeSetup(context: Context) = repository(context).completeSetup()

    /** Saves the personal account setup answers on the signed-in user. */
    suspend fun savePersonalSetup(context: Context, setup: PersonalSetup): Boolean =
        repository(context).savePersonalSetup(setup)

    /** Back to sign-up from the setup step: deletes the just-created account and signs out. */
    suspend fun discardNewAccount(context: Context): Boolean = repository(context).discardNewAccount()

    /** Signs the user out. MainActivity reacts and re-opens the sign-in screen. */
    suspend fun signOut(context: Context) = repository(context).signOut()
}

/** Launches [AuthenticationActivity]. Result is true if the user signed in. */
class AuthenticationContract : ActivityResultContract<Unit, Boolean>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, AuthenticationActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}
