package social.ahtara.authentication.domain






data class SocialAccount(
    val provider: SocialProvider,
    val subject: String,
    val email: String?,
    val displayName: String,
)