package social.ahtara.organization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import social.ahtara.websites.WebsiteFeedsSection





@Composable
fun OrganizationDashboard(ownerId: String, modifier: Modifier = Modifier) {
    val viewModel: OrganizationViewModel = viewModel(
        key = "organization-dashboard-$ownerId",
        factory = OrganizationViewModel.factory(ownerId),
    )
    val state by viewModel.state.collectAsState()
    OrganizationHomeScreen(
        state = state,
        onAddMember = viewModel::addMember,
        onAddTeam = viewModel::addTeam,
        onAddEvent = viewModel::addEvent,
        onDeleteMember = viewModel::deleteMember,
        onDeleteTeam = viewModel::deleteTeam,
        onDeleteEvent = viewModel::deleteEvent,
        modifier = modifier,
        
        extraContent = { item(key = "website-feeds") { WebsiteFeedsSection(ownerId = ownerId) } },
    )
}