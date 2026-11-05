package org.example.core.identityapi

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/**
 * Login / sign-up. [AccountType] is the type already chosen on the account type screen (null = ask in the flow);
 * with it, "Sign up" goes straight to that type's registration form. Result: true once signed in.
 *
 * Lives in identity-api so features can launch authentication without depending on `:authentication`.
 * The activity is reached by class name; its extra key and class name must match
 * `AuthenticationActivity` in `:authentication`.
 */
class AuthenticationContract : ActivityResultContract<AccountType?, Boolean>() {
    override fun createIntent(context: Context, input: AccountType?): Intent =
        Intent()
            .setClassName(context, AUTH_ACTIVITY_CLASS_NAME)
            .putExtra(EXTRA_ACCOUNT_TYPE, input?.name)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK

    companion object {
        const val AUTH_ACTIVITY_CLASS_NAME = "org.example.shared.authentication.ui.AuthenticationActivity"
        const val EXTRA_ACCOUNT_TYPE = "org.example.shared.authentication.PRESELECTED_ACCOUNT_TYPE"
    }
}
