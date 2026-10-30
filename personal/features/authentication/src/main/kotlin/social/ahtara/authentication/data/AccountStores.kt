package social.ahtara.authentication.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import social.ahtara.authentication.data.business.BusinessDatabase
import social.ahtara.authentication.data.personal.PersonalDatabase
import social.ahtara.authentication.domain.AccountType











internal class AccountStores(private val context: Context) {

    private val personal by lazy { PersonalDatabase.create(context).userDao() }
    private val business by lazy { BusinessDatabase.create(context).userDao() }
    

    private val ids = UserIdGenerator(context)
    private val legacyImporter = LegacyAuthImporter(context, ids, ::daoOf)
    private val mutex = Mutex()
    @Volatile private var ready = false

    suspend fun of(type: AccountType): UserDao {
        ensureReady()
        return daoOf(type)
    }

    
    suspend fun directory(): AccountDirectory {
        ensureReady()
        return AccountDirectory(AccountType.available.map(::daoOf))
    }

    
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