package social.ahtara.account

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.AuthUser
import social.ahtara.authentication.ui.AccountTypeBadge
import social.ahtara.authentication.ui.ProfileHeader
import social.ahtara.business.BusinessDashboard
import social.ahtara.organization.OrganizationDashboard
import social.ahtara.authentication.R as AuthR

private const val TabProfile = 0
private const val TabDashboard = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountScreen(user: AuthUser, onBack: () -> Unit, onSignOut: () -> Unit) {
    
    val hasDashboard = user.accountType != AccountType.PERSONAL
    var tab by rememberSaveable { mutableIntStateOf(TabProfile) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(AuthR.drawable.authentication_ic_back),
                            contentDescription = stringResource(R.string.account_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (hasDashboard) {
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(
                        selected = tab == TabProfile,
                        onClick = { tab = TabProfile },
                        text = { Text(stringResource(R.string.account_tab_profile)) },
                    )
                    Tab(
                        selected = tab == TabDashboard,
                        onClick = { tab = TabDashboard },
                        text = { Text(stringResource(R.string.account_tab_dashboard)) },
                    )
                }
            }
            if (!hasDashboard || tab == TabProfile) {
                ProfileContent(user = user, onSignOut = onSignOut)
            } else when (user.accountType) {
                AccountType.BUSINESS -> BusinessDashboard(ownerId = user.id)
                AccountType.ORGANIZATION -> OrganizationDashboard(ownerId = user.id)
                AccountType.PERSONAL -> Unit
            }
        }
    }
}

@Composable
private fun ProfileContent(user: AuthUser, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProfileHeader(coverPath = user.coverPath, photoPath = user.photoPath)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AccountTypeBadge(user.accountType, size = 56.dp)
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.account_section_profile), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.account_login, user.identifier))
                user.username?.let { Text(stringResource(R.string.account_username, it)) }
                user.email?.let { Text(stringResource(R.string.account_email, it)) }
                user.birthday?.let { Text(stringResource(R.string.account_birthday, it)) }
                user.details.industry?.let { Text(stringResource(R.string.account_industry, it)) }
                user.details.phone?.let { Text(stringResource(R.string.account_phone, it)) }
                user.details.registrationId?.let { Text(stringResource(R.string.account_registration, it)) }
                Text(stringResource(R.string.account_type, stringResource(user.accountType.title)))
            }
        }

        user.about?.takeIf { it.isNotBlank() }?.let { about ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.account_section_about), style = MaterialTheme.typography.titleMedium)
                    Text(about)
                }
            }
        }

        
        if (user.accountType == AccountType.PERSONAL) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.account_section_places), style = MaterialTheme.typography.titleMedium)
                    if (user.places.isEmpty()) {
                        Text(stringResource(R.string.account_places_empty))
                    } else {
                        user.places.forEachIndexed { index, place ->
                            Text(if (index == 0) stringResource(R.string.account_place_main, place.name) else place.name)
                        }
                    }
                }
            }
        }

        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.account_action_sign_out))
        }
    }
}