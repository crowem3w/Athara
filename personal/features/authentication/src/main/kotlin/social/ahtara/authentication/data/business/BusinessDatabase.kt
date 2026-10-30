package social.ahtara.authentication.data.business

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import social.ahtara.authentication.data.UserDao
import social.ahtara.authentication.data.UserEntity





@Database(entities = [UserEntity::class], version = 1, exportSchema = true)
internal abstract class BusinessDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "authentication_business.db"

        fun create(context: Context): BusinessDatabase =
            Room.databaseBuilder(context.applicationContext, BusinessDatabase::class.java, NAME).build()
    }
}