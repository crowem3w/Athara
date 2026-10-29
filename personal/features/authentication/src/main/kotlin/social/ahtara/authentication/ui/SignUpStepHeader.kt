package social.ahtara.authentication.ui

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
import social.ahtara.authentication.R

private val HeaderHeight = 48.dp








@Composable
fun SignUpStepHeader(
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
        
        
        StepProgressBar(
            currentStep = currentStep,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = HeaderHeight / 2 - StepProgressBarCenterY),
        )
    }
}