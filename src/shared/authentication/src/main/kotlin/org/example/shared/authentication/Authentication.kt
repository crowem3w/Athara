package org.example.shared.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import org.example.shared.authentication.data.AuthApi
import org.example.shared.authentication.data.PhoneProofStore
import org.example.shared.authentication.data.RemoteAuthRepository
import org.example.shared.authentication.data.RemotePhoneVerifier
import org.example.shared.authentication.ui.AccountTypeActivity
import org.example.shared.authentication.ui.AuthenticationActivity
import org.example.core.identityapi.AccountType
import org.example.core.identityapi.AuthRepository
import org.example.core.identityapi.Identity
import org.example.core.identityapi.PhoneVerifier
import org.example.core.network.Core








object Authentication {
    private val proofs = PhoneProofStore()

    @Volatile
    private var repository: AuthRepository? = null

    @Volatile
    private var verifier: PhoneVerifier? = null

    
    fun init() {
        Identity.provide { context -> repository(context) }
    }

    fun repository(context: Context): AuthRepository =
        repository ?: synchronized(this) {
            repository ?: RemoteAuthRepository.create(context.applicationContext, proofs).also { repository = it }
        }

    
    fun install(repository: AuthRepository) {
        this.repository = repository
    }

    
    fun phoneVerifier(): PhoneVerifier =
        verifier ?: synchronized(this) {
            verifier ?: RemotePhoneVerifier(Core.create(AuthApi::class.java), proofs).also { verifier = it }
        }

    fun installPhoneVerifier(verifier: PhoneVerifier) {
        this.verifier = verifier
    }
}


/** Account type selection. Result: the chosen [AccountType], or null if the user backed out. */
class AccountTypeContract : ActivityResultContract<Unit, AccountType?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, AccountTypeActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): AccountType? =
        if (resultCode != Activity.RESULT_OK) null
        else intent?.getStringExtra(AccountTypeActivity.EXTRA_ACCOUNT_TYPE)
            ?.let { name -> AccountType.entries.firstOrNull { it.name == name } }
}
