package social.ahtara.authentication.data.personal

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import social.ahtara.authentication.data.UserDao
import social.ahtara.authentication.data.UserEntity





@Database(entities = [UserEntity::class], version = 1, exportSchema = true)
internal abstract class PersonalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "authentication_personal.db"

        fun create(context: Context): PersonalDatabase =
            Room.databaseBuilder(context.applicationContext, PersonalDatabase::class.java, NAME).build()
    }
}