package soc.athara.app.shared.designsystem

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import soc.athara.app.core.syncengine.SyncCoordinator
import soc.athara.app.core.syncengine.SyncNotice
import soc.athara.app.core.syncengine.SyncNotices


@Composable
fun SyncNoticeHost() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val notices by SyncNotices.items.collectAsState()
    when (val notice = notices.firstOrNull()) {
        null -> Unit
        is SyncNotice.Rejected -> AlertDialog(
            onDismissRequest = { SyncNotices.dismiss(notice.id) },
            title = { Text(stringResource(R.string.designsystem_sync_rejected_title)) },
            text = {
                Text(
                    stringResource(
                        if (notice.reverted) R.string.designsystem_sync_rejected_reverted else R.string.designsystem_sync_rejected_removed,
                        notice.label,
                    ),
                )
            },
            confirmButton = { TextButton(onClick = { SyncNotices.dismiss(notice.id) }) { Text(stringResource(R.string.designsystem_ok)) } },
        )
        is SyncNotice.DeleteConflict -> AlertDialog(
            onDismissRequest = { scope.launch { SyncCoordinator.resolveDeleteConflict(context, notice, deleteAnyway = false) } },
            title = { Text(stringResource(R.string.designsystem_sync_conflict_title)) },
            text = { Text(stringResource(R.string.designsystem_sync_conflict_body, notice.label)) },
            confirmButton = {
                TextButton(onClick = { scope.launch { SyncCoordinator.resolveDeleteConflict(context, notice, deleteAnyway = true) } }) {
                    Text(stringResource(R.string.designsystem_sync_conflict_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { scope.launch { SyncCoordinator.resolveDeleteConflict(context, notice, deleteAnyway = false) } }) {
                    Text(stringResource(R.string.designsystem_sync_conflict_keep))
                }
            },
        )
    }
}
