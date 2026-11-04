package org.example.modules.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.core.syncengine.SyncCoordinator
import org.example.modules.organization.data.EventEntity
import org.example.modules.organization.data.MemberEntity
import org.example.modules.organization.data.OrganizationDao
import org.example.modules.organization.data.OrganizationDatabase
import org.example.modules.organization.data.TeamEntity
import org.example.modules.organization.data.deleteEvent
import org.example.modules.organization.data.deleteMember
import org.example.modules.organization.data.deleteTeam

internal data class OrganizationUiState(
    val members: List<MemberEntity> = emptyList(),
    val teams: List<TeamEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
) {
    
    val upcomingEvents: Int get() = LocalDate.now().toString().let { today -> events.count { it.date >= today } }
}

internal class OrganizationViewModel(
    private val dao: OrganizationDao,
    private val ownerId: String,
    
    private val onChanged: () -> Unit,
) : ViewModel() {

    val state: StateFlow<OrganizationUiState> =
        combine(dao.members(ownerId), dao.teams(ownerId), dao.events(ownerId)) { members, teams, events ->
            OrganizationUiState(members, teams, events)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrganizationUiState())

    fun addMember(name: String, role: String) {
        viewModelScope.launch {
            dao.insertMember(MemberEntity(ownerId = ownerId, name = name.trim(), role = role.trim().ifBlank { null }))
            onChanged()
        }
    }

    fun addTeam(name: String) {
        viewModelScope.launch {
            dao.insertTeam(TeamEntity(ownerId = ownerId, name = name.trim()))
            onChanged()
        }
    }

    fun addEvent(title: String, isoDate: String) {
        viewModelScope.launch {
            dao.insertEvent(EventEntity(ownerId = ownerId, title = title.trim(), date = isoDate))
            onChanged()
        }
    }

    fun deleteMember(id: Long) {
        viewModelScope.launch {
            dao.deleteMember(id, ownerId)
            onChanged()
        }
    }

    fun deleteTeam(id: Long) {
        viewModelScope.launch {
            dao.deleteTeam(id, ownerId)
            onChanged()
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            dao.deleteEvent(id, ownerId)
            onChanged()
        }
    }

    companion object {
        fun factory(ownerId: String) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                OrganizationViewModel(OrganizationDatabase.get(app).dao(), ownerId) { SyncCoordinator.requestSync(app) }
            }
        }
    }
}


internal fun parseIsoDate(input: String): String? =
    try {
        LocalDate.parse(input.trim()).toString()
    } catch (_: DateTimeParseException) {
        null
    }