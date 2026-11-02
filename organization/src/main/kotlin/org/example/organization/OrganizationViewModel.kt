package org.example.organization

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
import org.example.organization.data.EventEntity
import org.example.organization.data.MemberEntity
import org.example.organization.data.OrganizationDao
import org.example.organization.data.OrganizationDatabase
import org.example.organization.data.TeamEntity

internal data class OrganizationUiState(
    val members: List<MemberEntity> = emptyList(),
    val teams: List<TeamEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
) {
    /** Events dated today or later. */
    val upcomingEvents: Int get() = LocalDate.now().toString().let { today -> events.count { it.date >= today } }
}

internal class OrganizationViewModel(
    private val dao: OrganizationDao,
    private val ownerId: String,
) : ViewModel() {

    val state: StateFlow<OrganizationUiState> =
        combine(dao.members(ownerId), dao.teams(ownerId), dao.events(ownerId)) { members, teams, events ->
            OrganizationUiState(members, teams, events)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrganizationUiState())

    fun addMember(name: String, role: String) {
        viewModelScope.launch {
            dao.insertMember(MemberEntity(ownerId = ownerId, name = name.trim(), role = role.trim().ifBlank { null }))
        }
    }

    fun addTeam(name: String) {
        viewModelScope.launch { dao.insertTeam(TeamEntity(ownerId = ownerId, name = name.trim())) }
    }

    fun addEvent(title: String, isoDate: String) {
        viewModelScope.launch { dao.insertEvent(EventEntity(ownerId = ownerId, title = title.trim(), date = isoDate)) }
    }

    fun deleteMember(id: Long) {
        viewModelScope.launch { dao.deleteMember(id, ownerId) }
    }

    fun deleteTeam(id: Long) {
        viewModelScope.launch { dao.deleteTeam(id, ownerId) }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch { dao.deleteEvent(id, ownerId) }
    }

    companion object {
        fun factory(ownerId: String) = viewModelFactory {
            initializer {
                OrganizationViewModel(OrganizationDatabase.get(this[APPLICATION_KEY]!!).dao(), ownerId)
            }
        }
    }
}

/** Returns the normalized ISO date (yyyy-MM-dd), or null if [input] is not a valid date. */
internal fun parseIsoDate(input: String): String? =
    try {
        LocalDate.parse(input.trim()).toString()
    } catch (_: DateTimeParseException) {
        null
    }
