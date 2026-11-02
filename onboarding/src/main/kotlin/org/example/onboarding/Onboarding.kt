package org.example.onboarding

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.edit

/**
 * Public entry point of the :onboarding module.
 */
object Onboarding {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_COMPLETED = "completed"

    /** True once the user has tapped "Get Started" at least once. */
    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)

    internal fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_COMPLETED, true) }
    }
}

/**
 * Launches [OnboardingActivity]. Result is true if onboarding was completed,
 * false if the user backed out.
 */
class OnboardingContract : ActivityResultContract<Unit, Boolean>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, OnboardingActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == android.app.Activity.RESULT_OK
}
