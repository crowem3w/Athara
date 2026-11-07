package soc.athara.app.core.syncengine

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


sealed interface SyncNotice {
    val id: Long
    
    val label: String

    
    data class Rejected(override val id: Long, override val label: String, val reverted: Boolean) : SyncNotice

    
    data class DeleteConflict(
        override val id: Long,
        override val label: String,
        val resource: String,
        val clientId: String,
    ) : SyncNotice
}


object SyncNotices {
    private val nextId = AtomicLong(1)
    private val state = MutableStateFlow<List<SyncNotice>>(emptyList())
    val items: StateFlow<List<SyncNotice>> = state.asStateFlow()

    internal fun rejected(label: String, reverted: Boolean) {
        state.update { it + SyncNotice.Rejected(nextId.getAndIncrement(), label, reverted) }
    }

    internal fun deleteConflict(label: String, resource: String, clientId: String) {
        state.update { list ->
            if (list.any { it is SyncNotice.DeleteConflict && it.clientId == clientId }) list
            else list + SyncNotice.DeleteConflict(nextId.getAndIncrement(), label, resource, clientId)
        }
    }

    fun dismiss(id: Long) = state.update { list -> list.filterNot { it.id == id } }

    fun clear() = state.update { emptyList() }
}
