package soc.athara.app.modules.organization

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
import soc.athara.app.core.identityapi.AccountType
import soc.athara.app.shared.designsystem.AccountTypeBadge


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrganizationGuestHome(onLogin: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.organization_home_title)) },
                actions = { TextButton(onClick = onLogin) { Text(stringResource(R.string.organization_guest_login)) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            AccountTypeBadge(AccountType.ORGANIZATION, size = 72.dp)
            Text(
                text = stringResource(AccountType.ORGANIZATION.title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(AccountType.ORGANIZATION.description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.organization_guest_cta))
            }
        }
    }
}
