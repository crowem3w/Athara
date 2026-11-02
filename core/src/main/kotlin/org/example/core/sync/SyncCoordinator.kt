package org.example.core.sync

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
import org.example.core.Core
import org.example.core.net.ApiResult
import org.example.core.net.PushRequest
import org.example.core.net.apiCall

enum class SyncOutcome { SUCCESS, RETRY, SIGNED_OUT }

/**
 * Runs push-then-pull for every registered [SyncAdapter] and schedules it with WorkManager.
 * Pushes are idempotent (clientId), pulls are cursor-based, so running twice or being killed halfway is safe.
 */
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

    /** Sync soon, once the device is online. Safe to call after every local change. */
    fun requestSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(online())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // APPEND_OR_REPLACE: a change made while a sync is running gets its own follow-up run.
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
    }

    /**
     * Forget all pull cursors (next sync re-downloads everything). Call it whenever a local database
     * is wiped or recreated, otherwise records older than the cursor would never come back.
     */
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

        // 1) push local changes
        while (true) {
            val pending = adapter.pendingMutations(userId)
            if (pending.isEmpty()) break
            when (val result = apiCall { Core.api.push(adapter.resource, PushRequest(pending.take(BATCH))) }) {
                is ApiResult.Ok -> for (r in result.value.results) {
                    if (adapter.applyPushResult(userId, r)) fullResync = true
                }
                is ApiResult.Failure -> return failure(result)
            }
            // No progress (server skipped or half-answered the batch): stop and retry later instead of looping.
            if (adapter.pendingMutations(userId).size >= pending.size) return SyncOutcome.RETRY
        }

        // 2) pull the server's changes
        val context = Core.appContext
        var cursor = if (fullResync) null else prefs(context).getString(cursorKey(userId, adapter.resource), null)
        do {
            when (val result = apiCall { Core.api.pull(adapter.resource, cursor, PAGE) }) {
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
        !Core.tokens.signedIn.value -> SyncOutcome.SIGNED_OUT // refresh token was rejected
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
