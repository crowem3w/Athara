package social.ahtara.business

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import social.ahtara.websites.WebsiteFeedsSection





@Composable
fun BusinessDashboard(ownerId: String, modifier: Modifier = Modifier) {
    val viewModel: BusinessViewModel = viewModel(
        key = "business-dashboard-$ownerId",
        factory = BusinessViewModel.factory(ownerId),
    )
    val state by viewModel.state.collectAsState()
    BusinessHomeScreen(
        state = state,
        onAddCustomer = viewModel::addCustomer,
        onAddOrder = viewModel::addOrder,
        onDeleteCustomer = viewModel::deleteCustomer,
        onDeleteOrder = viewModel::deleteOrder,
        onSaveListing = viewModel::saveListing,
        onDeleteListing = viewModel::deleteListing,
        onAddEvent = viewModel::addEvent,
        onDeleteEvent = viewModel::deleteEvent,
        modifier = modifier,
        
        extraContent = { item(key = "website-feeds") { WebsiteFeedsSection(ownerId = ownerId) } },
    )
}