package soc.athara.app.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import soc.athara.app.shared.authentication.Authentication
import soc.athara.app.core.identityapi.Identity
import soc.athara.app.core.network.Core
import soc.athara.app.core.network.CoreConfig
import soc.athara.app.modules.organization.data.OrganizationSync
import soc.athara.app.core.syncengine.SyncCoordinator
import soc.athara.app.core.syncengine.SyncInstaller

class App : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    
    private val syncInstallers: List<SyncInstaller> = listOf(OrganizationSync)

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