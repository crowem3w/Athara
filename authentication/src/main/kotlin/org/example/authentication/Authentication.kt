package org.example.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.example.authentication.data.PhoneProofStore
import org.example.authentication.data.RemoteAuthRepository
import org.example.authentication.data.RemotePhoneVerifier
import org.example.authentication.domain.AuthRepository
import org.example.authentication.domain.PersonalSetup
import org.example.authentication.domain.PhoneVerifier
import org.example.authentication.ui.AuthenticationActivity
import org.example.core.Core

/**
 * Public entry point of the :authentication module.
 *
 * Needs `Core.init(...)` to have run (Application.onCreate). Uses a simple singleton provider (no
 * DI library); [install] / [installPhoneVerifier] replace the defaults, e.g. in tests.
 */
object Authentication {
    private val proofs = PhoneProofStore()

    @Volatile
    private var repository: AuthRepository? = null

    @Volatile
    private var verifier: PhoneVerifier? = null

    fun repository(context: Context): AuthRepository =
        repository ?: synchronized(this) {
            repository ?: RemoteAuthRepository.create(context.applicationContext, proofs).also { repository = it }
        }

    /** Replace the default repository (call before first use). */
    fun install(repository: AuthRepository) {
        this.repository = repository
    }

    /** SMS OTP verification. Default: the backend's /v1/auth/phone endpoints. */
    fun phoneVerifier(): PhoneVerifier =
        verifier ?: synchronized(this) {
            verifier ?: RemotePhoneVerifier(Core.api, proofs).also { verifier = it }
        }

    fun installPhoneVerifier(verifier: PhoneVerifier) {
        this.verifier = verifier
    }

    fun isSignedIn(context: Context): Boolean =
        repository(context).currentUser.value != null

    /** True while the signed-in user just created the account and has not finished or skipped the setup step. */
    fun isSetupPending(context: Context): Boolean = repository(context).isSetupPending()

    /** Marks the account setup step as finished or skipped. */
    fun completeSetup(context: Context) = repository(context).completeSetup()

    /** Saves the personal account setup answers on the signed-in user. False if it could not be saved (e.g. offline). */
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
