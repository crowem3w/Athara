package org.example.core.identityapi

/**
 * An account returned by a social provider, before it is linked to a local user.
 *
 * @param subject stable provider-side id (Google's `sub`); the key the account is linked by.
 */
data class SocialAccount(
    val provider: SocialProvider,
    val subject: String,
    val email: String?,
    val displayName: String,
    /** Provider-signed ID token. The backend verifies it; without one the account cannot be used. */
    val idToken: String? = null,
)
