package org.example.core.identityapi






data class SocialAccount(
    val provider: SocialProvider,
    val subject: String,
    val email: String?,
    val displayName: String,
    
    val idToken: String? = null,
)