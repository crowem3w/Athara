package social.ahtara.authentication.domain






data class SocialAccount(
    val provider: SocialProvider,
    val subject: String,
    val email: String?,
    val displayName: String,
    /** Google ID token; the backend verifies it. Null for demo accounts. */
    val idToken: String? = null,
)