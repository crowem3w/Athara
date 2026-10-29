package social.ahtara.authentication.data

import android.content.Context
import androidx.core.content.edit

/**
 * Globally unique user ids. Ids key photos, sessions and website ownership, so they must not
 * collide across the per-account-type databases; autoincrement per database cannot guarantee that.
 */
internal class UserIdGenerator(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun next(): Long = synchronized(prefs) {
        val id = prefs.getLong(KEY_NEXT_ID, 1L)
        prefs.edit(commit = true) { putLong(KEY_NEXT_ID, id + 1) }
        id
    }

    /** Makes sure future ids are greater than [maxId] (used after importing legacy rows). */
    fun reserveAbove(maxId: Long) = synchronized(prefs) {
        if (maxId + 1 > prefs.getLong(KEY_NEXT_ID, 1L)) prefs.edit(commit = true) { putLong(KEY_NEXT_ID, maxId + 1) }
    }

    private companion object {
        // Names unchanged on purpose: existing installs keep their counter.
        const val PREFS = "authentication_ids"
        const val KEY_NEXT_ID = "next_id"
    }
}
