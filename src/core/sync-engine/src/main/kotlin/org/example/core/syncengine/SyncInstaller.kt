package org.example.core.syncengine

import android.content.Context

/**
 * Implemented by every feature that owns synced tables. The app calls [install] once at startup
 * (see App.kt) and the feature registers its adapters with [SyncCoordinator]; the app never needs
 * to know which tables a feature has.
 */
interface SyncInstaller {
    fun install(context: Context)
}
