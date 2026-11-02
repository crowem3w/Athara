package org.example.authentication.domain

/**
 * Extra profile fields collected for Business and Organization accounts.
 * All null for Personal accounts.
 *
 * @property industry industry (Business) or organization type (Organization).
 * @property registrationId business registration / tax ID; optional.
 */
data class AccountDetails(
    val industry: String? = null,
    val phone: String? = null,
    val registrationId: String? = null,
)
