package org.example.authentication.data

import org.example.identityapi.PhoneVerifier
import org.example.network.ApiResult
import org.example.network.apiCall

/** SMS OTP through the backend (it owns the SMS provider). A successful verify stores the proof for the next request. */
internal class RemotePhoneVerifier(
    private val api: AuthApi,
    private val proofs: PhoneProofStore,
) : PhoneVerifier {

    override suspend fun sendCode(e164: String): Boolean =
        apiCall { api.sendCode(SendCodeRequest(e164)) } is ApiResult.Ok

    override suspend fun verify(e164: String, code: String): Boolean =
        when (val result = apiCall { api.verifyCode(VerifyCodeRequest(e164, code)) }) {
            is ApiResult.Ok -> { proofs.put(result.value.verificationToken); true }
            is ApiResult.Failure -> false
        }
}
