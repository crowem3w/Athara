package org.example.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.authentication.domain.AccountType
import org.example.authentication.domain.AuthUser
import org.example.authentication.ui.AccountTypeBadge
import org.example.authentication.ui.ProfileHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PersonalHomeScreen(user: AuthUser, onSignOut: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.personal_home_title)) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileHeader(coverPath = user.coverPath, photoPath = user.photoPath)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AccountTypeBadge(AccountType.PERSONAL, size = 56.dp)
                Text(
                    text = stringResource(R.string.personal_home_greeting, user.displayName),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.personal_profile_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.personal_profile_login, user.identifier))
                    Text(stringResource(R.string.personal_profile_type))
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.personal_tips_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.personal_tip_1))
                    Text(stringResource(R.string.personal_tip_2))
                    Text(stringResource(R.string.personal_tip_3))
                }
            }
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.personal_action_sign_out))
            }
        }
    }
}
