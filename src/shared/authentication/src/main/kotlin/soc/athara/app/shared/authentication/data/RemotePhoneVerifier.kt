package soc.athara.app.shared.authentication.data

import soc.athara.app.core.identityapi.PhoneVerifier
import soc.athara.app.core.network.ApiResult
import soc.athara.app.core.network.apiCall


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