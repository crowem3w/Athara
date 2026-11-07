package soc.athara.app.modules.personal

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import soc.athara.app.modules.personal.features.travel.model.PlaceCategory
import soc.athara.app.modules.personal.features.travel.model.ServiceArea
import soc.athara.app.core.identityapi.AuthUser
import soc.athara.app.modules.personal.home.HomeScreen
import soc.athara.app.modules.personal.home.components.CreateSheet
import soc.athara.app.modules.personal.home.components.BlePayCard
import soc.athara.app.modules.personal.home.components.PayOutcome
import soc.athara.app.modules.personal.home.components.PaymentResultDialog
import soc.athara.app.modules.personal.wallet.SetBalanceDialog
import soc.athara.app.modules.personal.wallet.formatMoney
import kotlinx.coroutines.launch
import soc.athara.app.shared.designsystem.theme.AtharaTheme
import soc.athara.app.shared.payments.PaymentResult
import soc.athara.app.shared.payments.Payments

private val CategorySaver = listSaver<Set<PlaceCategory>, String>(
    save = { selected -> selected.map { it.name } },
    restore = { names -> names.mapNotNull { PlaceCategory.fromName(it) }.toSet() },
)





@Composable
internal fun PersonalHomeScreen(user: AuthUser?, onSignOut: () -> Unit, onLogin: () -> Unit) {
    var categories by rememberSaveable(stateSaver = CategorySaver) { mutableStateOf(emptySet<PlaceCategory>()) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showSetBalance by rememberSaveable { mutableStateOf(false) }
    
    var cardMinor by rememberSaveable { mutableStateOf(0L) }
    
    var outcome by remember { mutableStateOf<PayOutcome?>(null) }

    
    val scope = rememberCoroutineScope()
    var balanceMinor by remember(user?.id) { mutableStateOf(0L) }
    val refreshBalance: suspend () -> Unit = {
        if (user != null) (Payments.wallet() as? PaymentResult.Ok)?.let { balanceMinor = it.value.balanceMinor }
    }
    LaunchedEffect(user?.id) { refreshBalance() }

    AtharaTheme {
        
        val blurRadius by animateDpAsState(if (cardMinor > 0) 18.dp else 0.dp, label = "homeBlur")
        Box {
            Box(modifier = Modifier.blur(blurRadius)) {
                HomeScreen(
                    
                    places = listOf(ServiceArea.default),
                    categories = categories,
                    onToggleCategory = { category ->
                        categories = if (category in categories) categories - category else categories + category
                    },
                    onClearCategories = { categories = emptySet() },
                    userInitial = user?.let { it.displayName.trim().firstOrNull()?.uppercase() ?: "?" },
                    
                    onProfileClick = { if (user == null) onLogin() else showProfile = true },
                    
                    onCreateClick = {
                        if (user == null) onLogin() else {
                            showCreate = true
                            scope.launch { refreshBalance() }
                        }
                    },
                    
                    onRequireAccount = { user != null },
                )
            }

            if (cardMinor > 0 && user != null) {
                val onClose = { cardMinor = 0L }
                val onOutcome = { result: PayOutcome ->
                    cardMinor = 0L
                    outcome = result
                    
                    scope.launch { refreshBalance() }
                    Unit
                }
                BlePayCard(amountMinor = cardMinor, onClose = onClose, onOutcome = onOutcome)
            }
        }

        if (showCreate && user != null) {
            
            CreateSheet(
                balanceMinor = balanceMinor,
                onDismiss = { showCreate = false },
                onPay = { minor ->
                    showCreate = false
                    cardMinor = minor
                },
            )
        }

        outcome?.let { PaymentResultDialog(outcome = it, onDone = { outcome = null }) }

        if (showProfile && user != null) {
            AlertDialog(
                onDismissRequest = { showProfile = false },
                title = { Text(user.displayName) },
                text = {
                    Column {
                        Text(stringResource(R.string.personal_profile_login, user.identifier))
                        Text(stringResource(R.string.personal_balance_line, formatMoney(balanceMinor)))
                        TextButton(onClick = { showProfile = false; showSetBalance = true }) {
                            Text(stringResource(R.string.personal_balance_set))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProfile = false; onSignOut() }) {
                        Text(stringResource(R.string.personal_action_sign_out))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProfile = false }) {
                        Text(stringResource(R.string.personal_profile_close))
                    }
                },
            )
        }

        if (showSetBalance && user != null) {
            SetBalanceDialog(
                currentMinor = balanceMinor,
                onSave = { minor ->
                    showSetBalance = false
                    
                    scope.launch {
                        when (val r = Payments.setTestBalance(minor)) {
                            is PaymentResult.Ok -> balanceMinor = r.value.balanceMinor
                            is PaymentResult.Failed -> outcome = PayOutcome.Failed(r.error)
                        }
                    }
                },
                onDismiss = { showSetBalance = false },
            )
        }
    }
}
