package social.ahtara.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private enum class OrganizationDialog { MEMBER, TEAM, EVENT }

@Composable
internal fun OrganizationHomeScreen(
    state: OrganizationUiState,
    onAddMember: (name: String, role: String) -> Unit,
    onAddTeam: (name: String) -> Unit,
    onAddEvent: (title: String, isoDate: String) -> Unit,
    onDeleteMember: (Long) -> Unit,
    onDeleteTeam: (Long) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    modifier: Modifier = Modifier,
    
    extraContent: LazyListScope.() -> Unit = {},
) {
    var dialog by remember { mutableStateOf<OrganizationDialog?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(R.string.organization_stat_members, state.members.size.toString(), Modifier.weight(1f))
                    StatTile(R.string.organization_stat_teams, state.teams.size.toString(), Modifier.weight(1f))
                    StatTile(R.string.organization_stat_events, state.upcomingEvents.toString(), Modifier.weight(1f))
                }
            }

            
            item { SectionHeader(R.string.organization_section_members) { dialog = OrganizationDialog.MEMBER } }
            if (state.members.isEmpty()) item { Text(stringResource(R.string.organization_empty_members), style = MaterialTheme.typography.bodyMedium) }
            items(state.members, key = { "member-${it.id}" }) { member ->
                ItemRow(member.name, member.role) { onDeleteMember(member.id) }
            }

            
            item { SectionHeader(R.string.organization_section_teams) { dialog = OrganizationDialog.TEAM } }
            if (state.teams.isEmpty()) item { Text(stringResource(R.string.organization_empty_teams), style = MaterialTheme.typography.bodyMedium) }
            items(state.teams, key = { "team-${it.id}" }) { team ->
                ItemRow(team.name, null) { onDeleteTeam(team.id) }
            }

            
            item { SectionHeader(R.string.organization_section_events) { dialog = OrganizationDialog.EVENT } }
            if (state.events.isEmpty()) item { Text(stringResource(R.string.organization_empty_events), style = MaterialTheme.typography.bodyMedium) }
            items(state.events, key = { "event-${it.id}" }) { event ->
                ItemRow(event.title, event.date) { onDeleteEvent(event.id) }
            }

            extraContent()
        }
    }

    when (dialog) {
        OrganizationDialog.MEMBER -> AddMemberDialog(onDismiss = { dialog = null }, onSave = onAddMember)
        OrganizationDialog.TEAM -> AddTeamDialog(onDismiss = { dialog = null }, onSave = onAddTeam)
        OrganizationDialog.EVENT -> AddEventDialog(onDismiss = { dialog = null }, onSave = onAddEvent)
        null -> Unit
    }
}

@Composable
private fun StatTile(label: Int, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionHeader(title: Int, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onAdd) { Text(stringResource(R.string.organization_action_add)) }
    }
}

@Composable
private fun ItemRow(primary: String, secondary: String?, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(primary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!secondary.isNullOrBlank()) Text(secondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.organization_action_delete)) }
        }
    }
}

@Composable
private fun AddMemberDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.organization_dialog_member_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; nameError = false },
                    label = { Text(stringResource(R.string.organization_label_member_name)) },
                    singleLine = true, isError = nameError,
                    supportingText = if (nameError) ({ Text(stringResource(R.string.organization_error_required)) }) else null,
                )
                OutlinedTextField(
                    value = role, onValueChange = { role = it },
                    label = { Text(stringResource(R.string.organization_label_member_role)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) nameError = true else { onSave(name, role); onDismiss() }
            }) { Text(stringResource(R.string.organization_action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.organization_action_cancel)) } },
    )
}

@Composable
private fun AddTeamDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.organization_dialog_team_title)) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it; nameError = false },
                label = { Text(stringResource(R.string.organization_label_team_name)) },
                singleLine = true, isError = nameError,
                supportingText = if (nameError) ({ Text(stringResource(R.string.organization_error_required)) }) else null,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) nameError = true else { onSave(name); onDismiss() }
            }) { Text(stringResource(R.string.organization_action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.organization_action_cancel)) } },
    )
}

@Composable
private fun AddEventDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var titleError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.organization_dialog_event_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it; titleError = false },
                    label = { Text(stringResource(R.string.organization_label_event_title)) },
                    singleLine = true, isError = titleError,
                    supportingText = if (titleError) ({ Text(stringResource(R.string.organization_error_required)) }) else null,
                )
                OutlinedTextField(
                    value = date, onValueChange = { date = it; dateError = false },
                    label = { Text(stringResource(R.string.organization_label_event_date)) },
                    singleLine = true, isError = dateError,
                    supportingText = if (dateError) ({ Text(stringResource(R.string.organization_error_date)) }) else null,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val iso = parseIsoDate(date)
                titleError = title.isBlank()
                dateError = iso == null
                if (!titleError && iso != null) { onSave(title, iso); onDismiss() }
            }) { Text(stringResource(R.string.organization_action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.organization_action_cancel)) } },
    )
}