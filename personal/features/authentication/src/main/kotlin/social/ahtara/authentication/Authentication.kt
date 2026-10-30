package social.ahtara.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import social.ahtara.authentication.data.DeviceFlowStore
import social.ahtara.authentication.data.LocalAuthRepository
import social.ahtara.authentication.data.RemoteAuthRepository
import social.ahtara.network.ApiConfig
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.AuthRepository
import social.ahtara.authentication.domain.NoVerification
import social.ahtara.authentication.domain.BusinessSetup
import social.ahtara.authentication.domain.PersonalSetup
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.PhoneVerifier
import social.ahtara.authentication.ui.AuthenticationActivity








object Authentication {
    @Volatile
    private var repository: AuthRepository? = null

    fun repository(context: Context): AuthRepository =
        repository ?: synchronized(this) {
            repository ?: createDefault(context.applicationContext).also { repository = it }
        }

    
    private fun createDefault(app: Context): AuthRepository =
        if (ApiConfig.backendEnabled(app)) RemoteAuthRepository.create(app) else LocalAuthRepository.create(app)

    fun install(repository: AuthRepository) {
        this.repository = repository
    }

    @Volatile
    var phoneVerifier: PhoneVerifier = NoVerification
        private set

    
    fun installPhoneVerifier(verifier: PhoneVerifier) {
        phoneVerifier = verifier
    }

    fun isSignedIn(context: Context): Boolean =
        repository(context).currentUser.value != null

    
    fun isSetupPending(context: Context): Boolean = repository(context).isSetupPending()

    
    fun completeSetup(context: Context) = repository(context).completeSetup()

    
    suspend fun savePersonalSetup(context: Context, setup: PersonalSetup): Boolean =
        repository(context).savePersonalSetup(setup)

    suspend fun saveBusinessSetup(context: Context, setup: BusinessSetup): Boolean =
        repository(context).saveBusinessSetup(setup)

    
    suspend fun savePlaces(context: Context, places: List<PlaceSelection>): Boolean =
        repository(context).savePlaces(places)

    
    suspend fun discardNewAccount(context: Context): Boolean = repository(context).discardNewAccount()

    
    suspend fun signOut(context: Context) = repository(context).signOut()

    

    private fun flow(context: Context) = DeviceFlowStore(context.applicationContext)

    
    fun selectedAccountType(context: Context): AccountType? =
        flow(context).selectedType() ?: AccountType.available.singleOrNull()

    fun selectAccountType(context: Context, type: AccountType) = flow(context).selectType(type)

    




    fun needsAccountTypeStep(context: Context): Boolean =
        AccountType.available.size > 1 && flow(context).selectedType() == null

    
    fun devicePlaces(context: Context): List<PlaceSelection> = flow(context).places()

    fun saveDevicePlaces(context: Context, places: List<PlaceSelection>) = flow(context).savePlaces(places)

    
    fun needsLocationStep(context: Context): Boolean =
        flow(context).let { it.places().isEmpty() && !it.isLocationStepDone() }

    
    fun markLocationStepDone(context: Context) = flow(context).markLocationStepDone()

    
    fun deviceAddress(context: Context): String = flow(context).address()

    fun saveDeviceAddress(context: Context, address: String) = flow(context).saveAddress(address)

    
}


enum class AuthEntry { SIGN_IN, SIGN_UP }


class AuthenticationContract : ActivityResultContract<AuthEntry, Boolean>() {
    override fun createIntent(context: Context, input: AuthEntry): Intent =
        Intent(context, AuthenticationActivity::class.java)
            .putExtra(AuthenticationActivity.EXTRA_ENTRY, input.name)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}