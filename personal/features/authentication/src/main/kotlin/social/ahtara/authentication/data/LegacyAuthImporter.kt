package social.ahtara.authentication.data

import android.content.Context
import social.ahtara.authentication.data.legacy.LegacyAuthDatabase
import social.ahtara.authentication.domain.AccountType
import java.io.File

/**
 * One-time upgrade step: copies users out of the pre-split single `authentication.db` into the
 * per-type databases (ids preserved) and keeps the old file as `authentication.db.migrated`.
 * Remove this class and `data/legacy/` once all installs have upgraded.
 */
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
                // Business / Organization have no database yet: skip them (the backup file below keeps
                // the rows). Remove this check once their databases exist.
                if (type !in AccountType.available) return@forEach
                val dao = daoOf(type)
                // Idempotent: a previous run may have been interrupted half-way.
                if (dao.findById(row.id) == null) dao.insert(row)
                if (row.id > maxId) maxId = row.id
            }
        } finally {
            legacy.close()
        }
        ids.reserveAbove(maxId)
        // Keep a backup instead of deleting user data.
        file.renameTo(File(file.parentFile, LegacyAuthDatabase.NAME + ".migrated"))
    }
}
