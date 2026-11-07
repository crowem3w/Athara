package soc.athara.app.core.identityapi

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract









class AuthenticationContract : ActivityResultContract<AccountType?, Boolean>() {
    override fun createIntent(context: Context, input: AccountType?): Intent =
        Intent()
            .setClassName(context, AUTH_ACTIVITY_CLASS_NAME)
            .putExtra(EXTRA_ACCOUNT_TYPE, input?.name)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK

    companion object {
        const val AUTH_ACTIVITY_CLASS_NAME = "soc.athara.app.shared.authentication.ui.AuthenticationActivity"
        const val EXTRA_ACCOUNT_TYPE = "soc.athara.app.shared.authentication.PRESELECTED_ACCOUNT_TYPE"
    }
}
