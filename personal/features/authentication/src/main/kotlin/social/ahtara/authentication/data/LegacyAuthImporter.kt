package social.ahtara.authentication.data

import android.content.Context
import social.ahtara.authentication.data.legacy.LegacyAuthDatabase
import social.ahtara.authentication.domain.AccountType
import java.io.File






internal class LegacyAuthImporter(
    private val context: Context,
    private val ids: UserIdGenerator,
    private val daoOf: (AccountType) -> UserDao,
) {
    suspend fun importIfPresent() {
        val file = context.getDatabasePath(LegacyAuthDatabase.NAME)
        if (!file.exists()) return
        val legacy = LegacyAuthDatabase.create(context)
        var maxId = 0L
        try {
            legacy.userDao().all().forEach { row ->
                val type = AccountType.fromName(row.accountType)
                
                
                if (type !in AccountType.available) return@forEach
                val dao = daoOf(type)
                
                if (dao.findById(row.id) == null) dao.insert(row)
                if (row.id > maxId) maxId = row.id
            }
        } finally {
            legacy.close()
        }
        ids.reserveAbove(maxId)
        
        file.renameTo(File(file.parentFile, LegacyAuthDatabase.NAME + ".migrated"))
    }
}