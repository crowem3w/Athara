package soc.athara.app.modules.personal.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import soc.athara.app.modules.personal.R
import soc.athara.app.modules.personal.wallet.AmountCalculator
import soc.athara.app.modules.personal.wallet.Currency
import soc.athara.app.modules.personal.wallet.PhilippinePeso
import soc.athara.app.modules.personal.wallet.SupportedCurrencies
import soc.athara.app.modules.personal.wallet.currencyByCode
import soc.athara.app.modules.personal.wallet.formatMinor
import soc.athara.app.shared.designsystem.theme.Background
import soc.athara.app.shared.designsystem.theme.SurfaceMuted
import soc.athara.app.shared.designsystem.theme.Teal
import soc.athara.app.shared.designsystem.theme.TealLight
import soc.athara.app.shared.designsystem.theme.TextPrimary
import soc.athara.app.shared.designsystem.theme.TextSecondary
import soc.athara.app.shared.designsystem.theme.TextTertiary

private val KeyHeight = 56.dp
private val KeyShape = RoundedCornerShape(16.dp)
private val KeyGap = 10.dp
private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * Bottom sheet opened by the centre button (replaces the old receive dialog).
 *
 * Top to bottom: currency picker, amount input, balance, operator row, 0-9 keypad, Pay.
 * Pay works for Philippine peso with an amount above zero that the balance covers: it closes the sheet and calls [onPay]
 * with the amount in minor units (the caller opens the Bluetooth pay card).
 *
 * TODO: balances are one fake PHP balance; other currencies show 0.00 until the ledger supports them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateSheet(balanceMinor: Long, onDismiss: () -> Unit, onPay: (Long) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var currencyCode by rememberSaveable { mutableStateOf(PhilippinePeso.code) }
    var input by rememberSaveable { mutableStateOf("") }

    val currency = currencyByCode(currencyCode)
    // Only PHP has a (fake) balance today.
    val shownBalance = if (currency.code == PhilippinePeso.code) balanceMinor else 0L
    val balanceText = "${currency.symbol} ${formatMinor(shownBalance)}"
    val balanceDescription = stringResource(R.string.personal_create_balance_desc, balanceText)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background,
        shape = SheetShape,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CurrencyPicker(currency = currency, onSelect = { currencyCode = it.code })

            AmountField(currency = currency, input = input)

            Text(
                text = balanceText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = balanceDescription },
            )

            OperatorRow(
                onOperator = { input = AmountCalculator.operator(input, it) },
                onEquals = { input = AmountCalculator.resolve(input) },
            )

            Keypad(
                onDigit = { input = AmountCalculator.digit(input, it) },
                onDot = { input = AmountCalculator.dot(input) },
                onBackspace = { input = AmountCalculator.backspace(input) },
                onClear = { input = "" },
            )

            val amountMinor = AmountCalculator.toMinor(input)
            val isPhp = currency.code == PhilippinePeso.code
            val enough = amountMinor <= balanceMinor
            val canPay = isPhp && amountMinor > 0 && enough
            val hint = when {
                !isPhp -> R.string.personal_create_hint_php_only
                amountMinor > 0 && !enough -> R.string.personal_create_hint_funds
                else -> null
            }
            if (hint != null) {
                Text(
                    text = stringResource(hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = {
                    // Slide the sheet away first, then hand over to the card.
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onPay(amountMinor) }
                },
                enabled = canPay,
                shape = KeyShape,
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.personal_create_pay), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CurrencyPicker(currency: Currency, onSelect: (Currency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.personal_create_currency_desc, currency.name)
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceMuted)
                .clickable(role = Role.DropdownList, onClick = { expanded = true })
                .semantics { contentDescription = description }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CurrencyBadge(currency, size = 32)
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(currency.code, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Text(currency.name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextSecondary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SupportedCurrencies.forEach { option ->
                DropdownMenuItem(
                    leadingIcon = { CurrencyBadge(option, size = 28) },
                    text = { Text("${option.code}  ·  ${option.name}") },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Round "currency icon": the currency sign on a soft teal disc. */
@Composable
private fun CurrencyBadge(currency: Currency, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(TealLight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = currency.symbol,
            color = Teal,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.45f).sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun AmountField(currency: Currency, input: String) {
    // Spaces around operators keep "120 + 30" readable; the stored string stays compact.
    val shown = buildString {
        input.forEach { c ->
            if (AmountCalculator.isOperator(c)) append(' ').append(c).append(' ') else append(c)
        }
    }
    val size = when {
        shown.length > 18 -> 22.sp
        shown.length > 11 -> 28.sp
        else -> 38.sp
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CurrencyBadge(currency, size = 44)
        Spacer(Modifier.size(12.dp))
        Text(
            text = shown.ifEmpty { "0" },
            style = MaterialTheme.typography.headlineMedium,
            fontSize = size,
            fontWeight = FontWeight.SemiBold,
            color = if (shown.isEmpty()) TextTertiary else TextPrimary,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OperatorRow(onOperator: (Char) -> Unit, onEquals: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
        listOf(
            AmountCalculator.Plus to R.string.personal_create_key_plus,
            AmountCalculator.Minus to R.string.personal_create_key_minus,
            AmountCalculator.Times to R.string.personal_create_key_times,
            AmountCalculator.Divide to R.string.personal_create_key_divide,
        ).forEach { (op, label) ->
            val description = stringResource(label)
            Key(
                container = TealLight,
                onClick = { onOperator(op) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = description },
            ) {
                Text(op.toString(), style = MaterialTheme.typography.titleLarge, color = Teal)
            }
        }
        val equalsDescription = stringResource(R.string.personal_create_key_equals)
        Key(
            container = Teal,
            onClick = onEquals,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = equalsDescription },
        ) {
            Text("=", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (Char) -> Unit,
    onDot: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(KeyGap)) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                row.forEach { d ->
                    Key(onClick = { onDigit(d) }, modifier = Modifier.weight(1f)) { KeyLabel(d.toString()) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
            Key(onClick = onDot, modifier = Modifier.weight(1f)) { KeyLabel(".") }
            Key(onClick = { onDigit('0') }, modifier = Modifier.weight(1f)) { KeyLabel("0") }
            // The "x" key: tap deletes the last character, long-press clears everything.
            Key(onClick = onBackspace, onLongClick = onClear, modifier = Modifier.weight(1f)) {
                Icon(
                    Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = stringResource(R.string.personal_create_key_delete),
                    tint = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun KeyLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Key(
    modifier: Modifier = Modifier,
    container: Color = SurfaceMuted,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .height(KeyHeight)
            .clip(KeyShape)
            .background(container)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
