package org.example.onboarding.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import org.example.onboarding.R

/**
 * Theme owned by the onboarding module. Light only for now, because the
 * background illustration has a white backdrop.
 */
@Composable
fun OnboardingTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = colorResource(R.color.onboarding_primary),
        onPrimary = colorResource(R.color.onboarding_on_primary),
        background = colorResource(R.color.onboarding_background),
        onBackground = colorResource(R.color.onboarding_on_background),
        surface = colorResource(R.color.onboarding_background),
        onSurface = colorResource(R.color.onboarding_on_background),
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}
