package org.example.core.syncengine

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Things the sync engine needs the user to know about or decide. Shown by `SyncNoticeHost` (design-system). */
sealed interface SyncNotice {
    val id: Long
    /** Human-readable name of the record (customer name, order title, ...). */
    val label: String

    /** The server refused [label]. [reverted] = an existing record went back to the server's version; otherwise a new record was removed. */
    data class Rejected(override val id: Long, override val label: String, val reverted: Boolean) : SyncNotice

    /** [label] was deleted here but changed elsewhere; the server's version was restored. The user chooses whether to delete it anyway. */
    data class DeleteConflict(
        override val id: Long,
        override val label: String,
        val resource: String,
        val clientId: String,
    ) : SyncNotice
}

/** In-memory queue (notices are lost if the process dies before they are shown). */
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
