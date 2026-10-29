package social.ahtara.authentication.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import social.ahtara.authentication.data.business.BusinessDatabase
import social.ahtara.authentication.data.personal.PersonalDatabase
import social.ahtara.authentication.domain.AccountType

/**
 * Routes an [AccountType] to its own Room database. PERSONAL and BUSINESS each live in their own database
 * file; ORGANIZATION is still a stub (see the TODO below).
 *
 * - [of] is the only way to reach one account type's data. Everything that happens after a user
 *   is located (profile updates, password changes, deletion) goes through it.
 * - [directory] is the ONLY cross-type door, and it is read-only identity lookup
 *   (see [AccountDirectory]); there is no "give me every DAO" escape hatch.
 * - The first call after an upgrade runs [LegacyAuthImporter].
 */
internal class AccountStores(private val context: Context) {

    private val personal by lazy { PersonalDatabase.create(context).userDao() }
    private val business by lazy { BusinessDatabase.create(context).userDao() }
    // TODO(organization): add data/organization/OrganizationDatabase.kt and expose its DAO here.

    private val ids = UserIdGenerator(context)
    private val legacyImporter = LegacyAuthImporter(context, ids, ::daoOf)
    private val mutex = Mutex()
    @Volatile private var ready = false

    suspend fun of(type: AccountType): UserDao {
        ensureReady()
        return daoOf(type)
    }

    /** Cross-type identity lookups only: sign-in and "is this identifier taken?". */
    suspend fun directory(): AccountDirectory {
        ensureReady()
        return AccountDirectory(AccountType.available.map(::daoOf))
    }

    /** Next globally unique user id. */
    suspend fun newId(): Long {
        ensureReady()
        return withContext(Dispatchers.IO) { ids.next() }
    }

    private fun daoOf(type: AccountType): UserDao = when (type) {
        AccountType.PERSONAL -> personal
        AccountType.BUSINESS -> business
        AccountType.ORGANIZATION -> TODO("Organization database not implemented yet")
    }

    private suspend fun ensureReady() {
        if (ready) return
        mutex.withLock {
            if (ready) return
            withContext(Dispatchers.IO) { legacyImporter.importIfPresent() }
            ready = true
        }
    }
}
