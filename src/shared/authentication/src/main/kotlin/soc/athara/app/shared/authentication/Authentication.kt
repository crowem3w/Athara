package soc.athara.app.shared.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import soc.athara.app.shared.authentication.data.PhoneProofStore
import soc.athara.app.shared.authentication.data.RemoteAuthRepository
import soc.athara.app.shared.authentication.ui.AccountTypeActivity
import soc.athara.app.shared.authentication.ui.AuthenticationActivity
import soc.athara.app.core.identityapi.AccountType
import soc.athara.app.core.identityapi.AuthRepository
import soc.athara.app.core.identityapi.Identity








object Authentication {
    private val proofs = PhoneProofStore()

    @Volatile
    private var repository: AuthRepository? = null

    
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
}



class AccountTypeContract : ActivityResultContract<Unit, AccountType?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, AccountTypeActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): AccountType? =
        if (resultCode != Activity.RESULT_OK) null
        else intent?.getStringExtra(AccountTypeActivity.EXTRA_ACCOUNT_TYPE)
            ?.let { name -> AccountType.entries.firstOrNull { it.name == name } }
}
