package org.example.authentication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserEntity::class], version = 9, exportSchema = true)
internal abstract class AuthDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        fun create(context: Context): AuthDatabase =
            Room.databaseBuilder(context.applicationContext, AuthDatabase::class.java, "authentication.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .build()

        /** v2: users get an account type; existing users become PERSONAL. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN accountType TEXT NOT NULL DEFAULT 'PERSONAL'")
            }
        }

        /** v3: business / organization details (all nullable). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN industry TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN phone TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN registrationId TEXT")
            }
        }

        /** v4: the `email` column becomes `identifier` (mobile numbers only; email sign-in removed). */
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

        /** v5: social accounts keep the provider-side id they are linked by. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN providerId TEXT")
            }
        }

        /**
         * v6: username + email columns, and `identifier` (mobile number) becomes nullable so an account
         * can exist without a mobile number. Existing rows keep their data; username / email are null.
         */
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

        /** v7: birthday (ISO date, nullable). Existing accounts have none. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN birthday TEXT")
            }
        }

        /** v9: cover photo file path, nullable. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN coverPath TEXT")
            }
        }

        /** v8: account setup answers (about, location, profile picture file path), all nullable. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN about TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN location TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN photoPath TEXT")
            }
        }
    }
}
