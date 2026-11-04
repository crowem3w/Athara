package org.example.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.example.shared.authentication.Authentication
import org.example.modules.business.data.BusinessSync
import org.example.core.identityapi.Identity
import org.example.core.network.Core
import org.example.core.network.CoreConfig
import org.example.modules.organization.data.OrganizationSync
import org.example.core.syncengine.SyncCoordinator
import org.example.core.syncengine.SyncInstaller

class App : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    
    private val syncInstallers: List<SyncInstaller> = listOf(BusinessSync, OrganizationSync)

    override fun onCreate() {
        super.onCreate()

        
        Core.init(
            this,
            CoreConfig(
                baseUrl = BuildConfig.API_BASE_URL,
                allowCleartext = BuildConfig.ALLOW_CLEARTEXT,
                httpLogging = BuildConfig.DEBUG,
                
            ),
        )

        
        Authentication.init()

        
        syncInstallers.forEach { it.install(this) }

        
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