package social.ahtara.authentication.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import social.ahtara.authentication.data.business.BusinessDatabase
import social.ahtara.authentication.data.legacy.LegacyAuthDatabase
import social.ahtara.authentication.data.organization.OrganizationDatabase
import social.ahtara.authentication.data.personal.PersonalDatabase
import social.ahtara.authentication.domain.AccountType
import java.io.File

/**
 * One data layer per account type: PERSONAL, BUSINESS and ORGANIZATION each live in their own
 * Room database file.
 *
 * - [of] is the only place that maps a type to its DAO; the repository never branches on type.
 * - [all] is for the few lookups that must span types (sign-in, "is this identifier taken?").
 * - User ids stay globally unique (they key photos, sessions and website ownership), so they come
 *   from one counter, [newId], instead of per-database autoincrement.
 * - The first call after an upgrade copies users out of the old single `authentication.db`
 *   (ids preserved) and keeps that file as `authentication.db.migrated`.
 */
internal class AccountStores(private val context: Context) {

    private val personal by lazy { PersonalDatabase.create(context).userDao() }
    private val business by lazy { BusinessDatabase.create(context).userDao() }
    private val organization by lazy { OrganizationDatabase.create(context).userDao() }

    private val idPrefs = context.applicationContext.getSharedPreferences("authentication_ids", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    @Volatile private var ready = false

    suspend fun of(type: AccountType): UserDao {
        ensureReady()
        return daoOf(type)
    }

    suspend fun all(): List<UserDao> {
        ensureReady()
        return AccountType.entries.map(::daoOf)
    }

    /** Next globally unique user id. */
    suspend fun newId(): Long {
        ensureReady()
        return withContext(Dispatchers.IO) {
            synchronized(idPrefs) {
                val id = idPrefs.getLong(KEY_NEXT_ID, 1L)
                idPrefs.edit(commit = true) { putLong(KEY_NEXT_ID, id + 1) }
                id
            }
        }
    }

    private fun daoOf(type: AccountType): UserDao = when (type) {
        AccountType.PERSONAL -> personal
        AccountType.BUSINESS -> business
        AccountType.ORGANIZATION -> organization
    }

    private suspend fun ensureReady() {
        if (ready) return
        mutex.withLock {
            if (ready) return
            withContext(Dispatchers.IO) { importLegacyUsers() }
            ready = true
        }
    }

    private suspend fun importLegacyUsers() {
        val file = context.getDatabasePath(LegacyAuthDatabase.NAME)
        if (!file.exists()) return
        val legacy = LegacyAuthDatabase.create(context)
        var maxId = 0L
        try {
            legacy.userDao().all().forEach { row ->
                val dao = daoOf(AccountType.fromName(row.accountType))
                // Idempotent: a previous run may have been interrupted half-way.
                if (dao.findById(row.id) == null) dao.insert(row)
                if (row.id > maxId) maxId = row.id
            }
        } finally {
            legacy.close()
        }
        synchronized(idPrefs) {
            val next = idPrefs.getLong(KEY_NEXT_ID, 1L)
            if (maxId + 1 > next) idPrefs.edit(commit = true) { putLong(KEY_NEXT_ID, maxId + 1) }
        }
        // Keep a backup instead of deleting user data.
        file.renameTo(File(file.parentFile, LegacyAuthDatabase.NAME + ".migrated"))
    }

    private companion object {
        const val KEY_NEXT_ID = "next_id"
    }
}
