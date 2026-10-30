package social.ahtara.authentication.data

import android.content.Context
import org.json.JSONObject
import social.ahtara.authentication.domain.PhoneVerifier
import social.ahtara.authentication.net.ApiClient
import social.ahtara.authentication.net.ApiException

/**
 * Phone OTP through `/auth/otp/send` + `/auth/otp/verify`. A successful [verify] keeps the returned
 * `verification_token` so the next sign-up for that number can attach it (needed when the backend runs
 * with `REQUIRE_PHONE_VERIFICATION=true`). Install it with `Authentication.installPhoneVerifier`.
 */
class RemotePhoneVerifier(context: Context, private val country: String = "PH") : PhoneVerifier {
    private val api = ApiClient.get(context)
    private val tokens = mutableMapOf<String, String>()

    override suspend fun sendCode(e164: String): Boolean = try {
        api.post("/auth/otp/send", JSONObject().put("phone", e164).put("country_iso", country), auth = false)
        true
    } catch (_: ApiException) {
        false
    }

    override suspend fun verify(e164: String, code: String): Boolean = try {
        val r = api.post(
            "/auth/otp/verify",
            JSONObject().put("phone", e164).put("country_iso", country).put("code", code),
            auth = false,
        ).obj()
        synchronized(tokens) { tokens[e164] = r.getString("verification_token") }
        true
    } catch (_: ApiException) {
        false
    }

    internal fun takeToken(identifier: String?): String? =
        identifier?.let { synchronized(tokens) { tokens.remove(it.trim()) } }
}
