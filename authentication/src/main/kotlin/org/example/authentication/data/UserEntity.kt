package org.example.authentication.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.example.authentication.domain.AccountDetails
import org.example.authentication.domain.AccountType
import org.example.authentication.domain.AuthUser

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["identifier"], unique = true),
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true),
    ],
)
internal data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /**
     * E.164 mobile number (or a demo social key). Always stored trimmed + lowercase. Null when the
     * account was created without a mobile number (Personal sign-up: it is optional).
     */
    val identifier: String?,
    val displayName: String,
    /** Null for social (demo) accounts. Format: iterations$salt$hash. */
    val passwordHash: String?,
    val provider: String,
    val createdAt: Long,
    /** [AccountType] name. Added in DB version 2. */
    @ColumnInfo(defaultValue = "'PERSONAL'") val accountType: String = AccountType.PERSONAL.name,
    /** Business / organization details. Added in DB version 3; null for personal accounts. */
    val industry: String? = null,
    val phone: String? = null,
    val registrationId: String? = null,
    /** Provider-side account id (Google `sub`) for linked social accounts. Added in DB version 5. */
    val providerId: String? = null,
    /** Lowercase sign-in handle. Added in DB version 6; null for accounts created before it existed. */
    val username: String? = null,
    /** Lowercase email, optional. Added in DB version 6. */
    val email: String? = null,
    /** ISO date (yyyy-MM-dd). Added in DB version 7; null for accounts created before it existed. */
    val birthday: String? = null,
    /** Account setup answers. Added in DB version 8; null until the user fills them in. */
    val about: String? = null,
    val location: String? = null,
    /** Absolute path of the profile picture in app storage. */
    val photoPath: String? = null,
    /** Absolute path of the cover photo in app storage. Added in DB version 9. */
    val coverPath: String? = null,
)

internal fun UserEntity.toAuthUser() = AuthUser(
    id = id.toString(),
    // What the profile screens show as "Login": username first, then mobile number, then email.
    identifier = username ?: identifier ?: email.orEmpty(),
    username = username,
    email = email,
    birthday = birthday,
    about = about,
    location = location,
    photoPath = photoPath,
    coverPath = coverPath,
    displayName = displayName,
    provider = provider,
    accountType = AccountType.fromName(accountType),
    details = AccountDetails(industry = industry, phone = phone, registrationId = registrationId),
)
