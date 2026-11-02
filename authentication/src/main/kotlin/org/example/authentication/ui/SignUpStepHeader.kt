package org.example.authentication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.example.authentication.R

private val HeaderHeight = 48.dp

/**
 * Top row of every sign-up step: the back button at the start and the step progress bar centered
 * in the same row. Shared by the account type screen and the registration forms so both sit in
 * exactly the same place.
 *
 * The square on the bars hops to the new bar whenever [currentStep] changes, forward or back.
 */
@Composable
internal fun SignUpStepHeader(
    currentStep: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth().height(HeaderHeight)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                painter = painterResource(R.drawable.authentication_ic_back),
                contentDescription = stringResource(R.string.authentication_action_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        // The strip is taller than the row (it holds the hopping square above the bars), so it is
        // lifted until the bars line up with the middle of the back button.
        StepProgressBar(
            currentStep = currentStep,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = HeaderHeight / 2 - StepProgressBarCenterY),
        )
    }
}
