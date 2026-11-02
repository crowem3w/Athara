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
import org.example.core.Core
import org.example.core.CoreConfig
import org.example.core.sync.SyncCoordinator
import org.example.organization.data.OrganizationSync

class App : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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

        // 2) tell the sync engine which tables exist
        BusinessSync.install(this)
        OrganizationSync.install(this)

        // 3) sync while someone is signed in; stop when signed out
        scope.launch {
            Authentication.repository(this@App).currentUser
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
