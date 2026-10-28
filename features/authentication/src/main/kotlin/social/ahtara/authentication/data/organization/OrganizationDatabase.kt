package social.ahtara.authentication.data.organization

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import social.ahtara.authentication.data.UserDao
import social.ahtara.authentication.data.UserEntity

/**
 * Storage for ORGANIZATION accounts only. Its schema can evolve independently of the other account types
 * (add tables / migrations here without touching them).
 */
@Database(entities = [UserEntity::class], version = 1, exportSchema = true)
internal abstract class OrganizationDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "authentication_organization.db"

        fun create(context: Context): OrganizationDatabase =
            Room.databaseBuilder(context.applicationContext, OrganizationDatabase::class.java, NAME).build()
    }
}
