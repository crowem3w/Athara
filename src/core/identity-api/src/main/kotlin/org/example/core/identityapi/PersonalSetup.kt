package org.example.core.identityapi

/** What the personal account setup step collects. Every field is optional. */
data class PersonalSetup(
    /** Content URI of the chosen profile picture, null if none. */
    val photoUri: String? = null,
    /** Content URI of the chosen cover photo, null if none. */
    val coverUri: String? = null,
    val about: String = "",
    val location: String = "",
)
