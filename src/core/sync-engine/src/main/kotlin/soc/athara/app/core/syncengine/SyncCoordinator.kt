package soc.athara.app.core.syncengine

import android.content.Context
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import soc.athara.app.core.network.Core
import soc.athara.app.core.network.ApiResult
import soc.athara.app.core.network.PushRequest
import soc.athara.app.core.network.apiCall

enum class SyncOutcome { SUCCESS, RETRY, SIGNED_OUT }





object SyncCoordinator {
    private const val BATCH = 50
    private const val PAGE = 200
    private const val WORK_NOW = "core_sync_now"
    private const val WORK_PERIODIC = "core_sync_periodic"

    private val adapters = CopyOnWriteArrayList<SyncAdapter>()
    private val mutex = Mutex()

    fun register(adapter: SyncAdapter) {
        if (adapters.none { it.resource == adapter.resource }) adapters.add(adapter)
    }

    
    fun requestSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(online())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).setConstraints(online()).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelAll(context: Context) {
        val work = WorkManager.getInstance(context)
        work.cancelUniqueWork(WORK_NOW)
        work.cancelUniqueWork(WORK_PERIODIC)
        SyncNotices.clear()
    }

    /** The user's answer to a [SyncNotice.DeleteConflict]: delete the record anyway, or keep the restored one. */
    suspend fun resolveDeleteConflict(context: Context, notice: SyncNotice.DeleteConflict, deleteAnyway: Boolean) {
        SyncNotices.dismiss(notice.id)
        if (!deleteAnyway) return
        val userId = Core.tokens.userId ?: return
        val adapter = adapters.firstOrNull { it.resource == notice.resource } ?: return
        if (adapter.requestDelete(userId, notice.clientId)) requestSync(context)
    }

    



    fun resetCursors(context: Context) = prefs(context).edit { clear() }

    suspend fun syncNow(): SyncOutcome = mutex.withLock {
        val userId = Core.tokens.userId ?: return SyncOutcome.SIGNED_OUT
        var retry = false
        for (adapter in adapters) {
            when (syncResource(adapter, userId)) {
                SyncOutcome.SIGNED_OUT -> return SyncOutcome.SIGNED_OUT
                SyncOutcome.RETRY -> retry = true
                SyncOutcome.SUCCESS -> Unit
            }
        }
        if (retry) SyncOutcome.RETRY else SyncOutcome.SUCCESS
    }

    private suspend fun syncResource(adapter: SyncAdapter, userId: String): SyncOutcome {
        var fullResync = false

        
        while (true) {
            val pending = adapter.pendingMutations(userId)
            if (pending.isEmpty()) break
            when (val result = apiCall { Core.sync.push(adapter.resource, PushRequest(pending.take(BATCH))) }) {
                is ApiResult.Ok -> for (r in result.value.results) {
                    if (adapter.applyPushResult(userId, r)) fullResync = true
                }
                is ApiResult.Failure -> return failure(result)
            }
            
            if (adapter.pendingMutations(userId).size >= pending.size) return SyncOutcome.RETRY
        }

        
        val context = Core.appContext
        var cursor = if (fullResync) null else prefs(context).getString(cursorKey(userId, adapter.resource), null)
        do {
            when (val result = apiCall { Core.sync.pull(adapter.resource, cursor, PAGE) }) {
                is ApiResult.Ok -> {
                    adapter.applyRemote(userId, result.value.records)
                    cursor = result.value.cursor ?: cursor
                    prefs(context).edit { putString(cursorKey(userId, adapter.resource), cursor) }
                    if (!result.value.hasMore) break
                }
                is ApiResult.Failure -> return failure(result)
            }
        } while (true)
        return SyncOutcome.SUCCESS
    }

    private fun failure(result: ApiResult.Failure): SyncOutcome = when {
        !Core.tokens.signedIn.value -> SyncOutcome.SIGNED_OUT 
        else -> SyncOutcome.RETRY
    }

    private fun online() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    private fun cursorKey(userId: String, resource: String) = "$userId/$resource"
    private fun prefs(context: Context) = context.getSharedPreferences("core_sync_cursors", Context.MODE_PRIVATE)
}

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (SyncCoordinator.syncNow()) {
        SyncOutcome.SUCCESS, SyncOutcome.SIGNED_OUT -> Result.success()
        SyncOutcome.RETRY -> Result.retry()
    }
}