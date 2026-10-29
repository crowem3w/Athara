package social.ahtara.authentication.domain







interface PhoneVerifier {
    
    suspend fun sendCode(e164: String): Boolean

    
    suspend fun verify(e164: String, code: String): Boolean
}

object NoVerification : PhoneVerifier {
    override suspend fun sendCode(e164: String) = true
    override suspend fun verify(e164: String, code: String) = true
}