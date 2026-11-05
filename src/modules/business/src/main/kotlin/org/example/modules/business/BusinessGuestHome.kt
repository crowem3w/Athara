package org.example.modules.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.example.core.identityapi.AccountType
import org.example.shared.designsystem.AccountTypeBadge

/** Home shown to a signed-out visitor who chose the business type. The top bar and the button open login / sign-up. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BusinessGuestHome(onLogin: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.business_home_title)) },
                actions = { TextButton(onClick = onLogin) { Text(stringResource(R.string.business_guest_login)) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            AccountTypeBadge(AccountType.BUSINESS, size = 72.dp)
            Text(
                text = stringResource(AccountType.BUSINESS.title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(AccountType.BUSINESS.description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.business_guest_cta))
            }
        }
    }
}
