package social.ahtara.authentication

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import social.ahtara.authentication.data.LocalAuthRepository
import social.ahtara.authentication.domain.AuthRepository
import social.ahtara.authentication.domain.NoVerification
import social.ahtara.authentication.domain.PersonalSetup
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.PhoneVerifier
import social.ahtara.authentication.ui.AuthenticationActivity








object Authentication {
    @Volatile
    private var repository: AuthRepository? = null

    fun repository(context: Context): AuthRepository =
        repository ?: synchronized(this) {
            repository ?: LocalAuthRepository.create(context.applicationContext).also { repository = it }
        }

    
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

    
    suspend fun savePlaces(context: Context, places: List<PlaceSelection>): Boolean =
        repository(context).savePlaces(places)

    
    suspend fun discardNewAccount(context: Context): Boolean = repository(context).discardNewAccount()

    
    suspend fun signOut(context: Context) = repository(context).signOut()
}


class AuthenticationContract : ActivityResultContract<Unit, Boolean>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, AuthenticationActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
        resultCode == Activity.RESULT_OK
}