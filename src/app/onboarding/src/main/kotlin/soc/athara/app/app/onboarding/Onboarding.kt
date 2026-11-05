package soc.athara.app.app.onboarding

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.edit




object Onboarding {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_COMPLETED = "completed"

    
    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)

    internal fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_COMPLETED, true) }
    }
}





class OnboardingContract : ActivityResultContract<Unit, Boolean>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, OnboardingActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == android.app.Activity.RESULT_OK
}