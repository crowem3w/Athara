package org.example.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.example.authentication.data.AuthApi
import org.example.authentication.data.PhoneProofStore
import org.example.authentication.data.RemoteAuthRepository
import org.example.authentication.data.RemotePhoneVerifier
import org.example.authentication.ui.AuthenticationActivity
import org.example.identityapi.AuthRepository
import org.example.identityapi.Identity
import org.example.identityapi.PhoneVerifier
import org.example.network.Core

/**
 * Public entry point of the :authentication module.
 *
 * Needs `Core.init(...)` to have run (Application.onCreate), then [init] so other modules can reach
 * the repository through `Identity` (:identity-api). Uses a simple singleton provider (no DI
 * library); [install] / [installPhoneVerifier] replace the defaults, e.g. in tests.
 */
object Authentication {
    private val proofs = PhoneProofStore()

    @Volatile
    private var repository: AuthRepository? = null

    @Volatile
    private var verifier: PhoneVerifier? = null

    /** Registers this module as the provider behind `Identity`. Call once in Application.onCreate, after `Core.init`. */
    fun init() {
        Identity.provide { context -> repository(context) }
    }

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
            verifier ?: RemotePhoneVerifier(Core.create(AuthApi::class.java), proofs).also { verifier = it }
        }

    fun installPhoneVerifier(verifier: PhoneVerifier) {
        this.verifier = verifier
    }
}

/** Launches [AuthenticationActivity]. Result is true if the user signed in. */
class AuthenticationContract : ActivityResultContract<Unit, Boolean>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, AuthenticationActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}
