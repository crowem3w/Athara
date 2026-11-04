package org.example.test

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.example.authentication.Authentication
import org.example.business.data.BusinessSync
import org.example.identityapi.Identity
import org.example.network.Core
import org.example.network.CoreConfig
import org.example.organization.data.OrganizationSync
import org.example.syncengine.SyncCoordinator
import org.example.syncengine.SyncInstaller

class App : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Every feature that owns synced tables. Adding a feature = adding one entry here. */
    private val syncInstallers: List<SyncInstaller> = listOf(BusinessSync, OrganizationSync)

    override fun onCreate() {
        super.onCreate()

        // 1) networking + secure storage (must come first)
        Core.init(
            this,
            CoreConfig(
                baseUrl = BuildConfig.API_BASE_URL,
                allowCleartext = BuildConfig.ALLOW_CLEARTEXT,
                httpLogging = BuildConfig.DEBUG,
                // certificatePins = mapOf("api.example.org" to listOf("sha256/...")), // enable for staging/prod
            ),
        )

        // 2) let feature modules reach the signed-in user through :identity-api
        Authentication.init()

        // 3) tell the sync engine which tables exist
        syncInstallers.forEach { it.install(this) }

        // 4) sync while someone is signed in; stop when signed out
        scope.launch {
            Identity.repository(this@App).currentUser
                .map { it?.id }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId != null) {
                        SyncCoordinator.schedulePeriodic(this@App)
                        SyncCoordinator.requestSync(this@App)
                    } else {
                        SyncCoordinator.cancelAll(this@App)
                    }
                }
        }
    }
}
