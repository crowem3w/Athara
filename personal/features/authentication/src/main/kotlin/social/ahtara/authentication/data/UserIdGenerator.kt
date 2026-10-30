package social.ahtara.authentication.data

import android.content.Context
import androidx.core.content.edit





internal class UserIdGenerator(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun next(): Long = synchronized(prefs) {
        val id = prefs.getLong(KEY_NEXT_ID, 1L)
        prefs.edit(commit = true) { putLong(KEY_NEXT_ID, id + 1) }
        id
    }

    
    fun reserveAbove(maxId: Long) = synchronized(prefs) {
        if (maxId + 1 > prefs.getLong(KEY_NEXT_ID, 1L)) prefs.edit(commit = true) { putLong(KEY_NEXT_ID, maxId + 1) }
    }

    private companion object {
        
        const val PREFS = "authentication_ids"
        const val KEY_NEXT_ID = "next_id"
    }
}