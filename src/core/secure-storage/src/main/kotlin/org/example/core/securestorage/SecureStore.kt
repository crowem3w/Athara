package org.example.core.securestorage

import android.content.Context
import androidx.core.content.edit

/**
 * Small encrypted key-value store (values are encrypted with [KeystoreCipher]; keys are not secret).
 * Use it for tokens and cached profile data, never for large blobs.
 */
class SecureStore(context: Context, name: String = "core_secure_store") {
    private val prefs = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun getString(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        val plain = KeystoreCipher.decrypt(stored, key)
        if (plain == null) prefs.edit { remove(key) } // unreadable: drop it so the user simply signs in again
        return plain
    }

    fun putString(key: String, value: String?) {
        if (value == null) remove(key) else prefs.edit { putString(key, KeystoreCipher.encrypt(value, key)) }
    }

    fun remove(key: String) = prefs.edit { remove(key) }

    fun clear() = prefs.edit { clear() }
}
