package social.ahtara.authentication.domain






data class SocialAccount(
    val provider: SocialProvider,
    val subject: String,
    val email: String?,
    val displayName: String,
    /** Provider-issued ID token; the backend verifies it. Null for providers without one. */
    val idToken: String? = null,
)