package social.ahtara.authentication.social

import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import social.ahtara.authentication.R
import social.ahtara.authentication.domain.SocialAccount
import social.ahtara.authentication.domain.SocialProvider
import org.json.JSONObject

internal sealed interface GoogleResult {
    data class Success(val account: SocialAccount) : GoogleResult
    data object Cancelled : GoogleResult
    data object NotConfigured : GoogleResult
    data object NoAccount : GoogleResult
    data object Failed : GoogleResult
}








internal object GoogleAuth {

    suspend fun signIn(activityContext: Context): GoogleResult {
        val clientId = activityContext.getString(R.string.authentication_google_web_client_id)
        if (clientId.isBlank() || clientId.startsWith("YOUR_")) return GoogleResult.NotConfigured

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(false) 
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val result = CredentialManager.create(activityContext).getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleResult.Success(
                    SocialAccount(
                        provider = SocialProvider.GOOGLE,
                        subject = subjectOf(google.idToken) ?: google.id,
                        email = google.id,
                        displayName = google.displayName.orEmpty(),
                        idToken = google.idToken,
                    )
                )
            } else {
                GoogleResult.Failed
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleResult.NoAccount
        } catch (_: GoogleIdTokenParsingException) {
            GoogleResult.Failed
        } catch (_: GetCredentialException) {
            GoogleResult.Failed
        }
    }

    
    private fun subjectOf(idToken: String): String? = runCatching {
        val payload = idToken.split(".")[1]
        val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        JSONObject(json).getString("sub")
    }.getOrNull()
}