package social.ahtara.authentication.data.legacy

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import social.ahtara.authentication.data.UserDao
import social.ahtara.authentication.data.UserEntity

/** Pre-split single database. Only read once to copy users into the per-type databases, then retired. */
@Database(entities = [UserEntity::class], version = 11, exportSchema = true)
internal abstract class LegacyAuthDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "authentication.db"

        fun create(context: Context): LegacyAuthDatabase =
            Room.databaseBuilder(context.applicationContext, LegacyAuthDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .build()

        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN accountType TEXT NOT NULL DEFAULT 'PERSONAL'")
            }
        }

        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN industry TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN phone TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN registrationId TEXT")
            }
        }

        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE users_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, identifier TEXT NOT NULL, " +
                        "displayName TEXT NOT NULL, passwordHash TEXT, provider TEXT NOT NULL, createdAt INTEGER NOT NULL, " +
                        "accountType TEXT NOT NULL DEFAULT 'PERSONAL', industry TEXT, phone TEXT, registrationId TEXT)"
                )
                db.execSQL(
                    "INSERT INTO users_new (id, identifier, displayName, passwordHash, provider, createdAt, accountType, industry, phone, registrationId) " +
                        "SELECT id, email, displayName, passwordHash, provider, createdAt, accountType, industry, phone, registrationId FROM users"
                )
                db.execSQL("DROP TABLE users")
                db.execSQL("ALTER TABLE users_new RENAME TO users")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_identifier ON users (identifier)")
            }
        }

        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN providerId TEXT")
            }
        }

        



        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE users_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, identifier TEXT, " +
                        "displayName TEXT NOT NULL, passwordHash TEXT, provider TEXT NOT NULL, createdAt INTEGER NOT NULL, " +
                        "accountType TEXT NOT NULL DEFAULT 'PERSONAL', industry TEXT, phone TEXT, registrationId TEXT, " +
                        "providerId TEXT, username TEXT, email TEXT)"
                )
                db.execSQL(
                    "INSERT INTO users_new (id, identifier, displayName, passwordHash, provider, createdAt, accountType, industry, phone, registrationId, providerId) " +
                        "SELECT id, identifier, displayName, passwordHash, provider, createdAt, accountType, industry, phone, registrationId, providerId FROM users"
                )
                db.execSQL("DROP TABLE users")
                db.execSQL("ALTER TABLE users_new RENAME TO users")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_identifier ON users (identifier)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_username ON users (username)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users (email)")
            }
        }

        
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN birthday TEXT")
            }
        }

        
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN placesJson TEXT")
            }
        }

        
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE users ADD COLUMN longitude REAL")
            }
        }

        
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN coverPath TEXT")
            }
        }

        
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN about TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN location TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN photoPath TEXT")
            }
        }
    }
}