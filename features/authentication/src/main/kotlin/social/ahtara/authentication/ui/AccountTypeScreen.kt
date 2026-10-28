package social.ahtara.authentication.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.R
import social.ahtara.authentication.domain.AccountType

private val CardMinHeight = 172.dp
private val CardMaxHeight = 220.dp


private val NonCardHeight = 120.dp








@Composable
internal fun AccountTypeScreen(
    onSelect: (AccountType) -> Unit,
    onSignIn: (() -> Unit)?,
    types: List<AccountType> = AccountType.available,
) {
    // With a single offered type (personal only) it is preselected.
    var selectedName by rememberSaveable { mutableStateOf<String?>(types.singleOrNull()?.name) }
    val selected = selectedName?.let { AccountType.fromName(it) }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val cardHeight: Dp = ((maxHeight - NonCardHeight - 32.dp) / types.size)
                .coerceIn(CardMinHeight, CardMaxHeight)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.authentication_account_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.authentication_account_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(R.color.authentication_muted),
                    )
                }

                types.forEach { type ->
                    AccountTypeCard(
                        type = type,
                        isSelected = selected == type,
                        hasSelection = selected != null,
                        onClick = { selectedName = type.name },
                        modifier = Modifier.fillMaxWidth().height(cardHeight),
                    )
                }
            }
        }

        val accent = selected?.let { colorResource(it.accentColor) } ?: MaterialTheme.colorScheme.primary
        Button(
            onClick = { selected?.let(onSelect) },
            enabled = selected != null,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Color.White,
                disabledContainerColor = colorResource(R.color.authentication_field_border),
                disabledContentColor = colorResource(R.color.authentication_muted),
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(
                text = stringResource(R.string.authentication_action_continue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (onSignIn != null) {
            TextButton(
                onClick = onSignIn,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp),
            ) {
                Text(stringResource(R.string.authentication_action_to_sign_in))
            }
        }
    }
}

@Composable
private fun AccountTypeCard(
    type: AccountType,
    isSelected: Boolean,
    hasSelection: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = colorResource(type.accentColor)
    val spec = tween<Float>(200)

    val elevation by animateDpAsState(if (isSelected) 12.dp else 0.dp, tween(200), label = "elevation")
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (hasSelection && !isSelected) 0.55f else 1f,
        animationSpec = spec,
        label = "alpha",
    )
    val borderColor by animateColorAsState(
        if (isSelected) accent else colorResource(R.color.authentication_field_border),
        tween(200),
        label = "border",
    )
    val fill by animateColorAsState(
        if (isSelected) accent.copy(alpha = 0.08f).compositeOver(Color.White)
        else colorResource(R.color.authentication_field_background),
        tween(200),
        label = "fill",
    )

    Surface(
        modifier = modifier
            .alpha(contentAlpha)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = fill,
        shadowElevation = elevation,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AccountTypeShape(
                type = type,
                color = colorResource(R.color.authentication_primary),
                modifier = Modifier.fillMaxSize(),
            )
            
            
            Image(
                painter = painterResource(type.illustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterEnd,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.50f)
                    .padding(top = 10.dp, bottom = 10.dp, end = 8.dp),
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.55f)
                    .padding(start = 24.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AccountTypeBadge(type, size = 38.dp)
                Text(
                    text = stringResource(type.title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(type.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorResource(R.color.authentication_muted),
                )
            }
            
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                SelectionIndicator(isSelected = isSelected, accent = accent)
            }
        }
    }
}


@Composable
private fun SelectionIndicator(isSelected: Boolean, accent: Color) {
    val fill by animateColorAsState(if (isSelected) accent else Color.Transparent, tween(200), label = "indicator")
    val ring by animateColorAsState(
        if (isSelected) accent else colorResource(R.color.authentication_muted).copy(alpha = 0.5f),
        tween(200),
        label = "ring",
    )
    Box(
        modifier = Modifier.size(28.dp).background(fill, CircleShape).border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.authentication_ic_check),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}