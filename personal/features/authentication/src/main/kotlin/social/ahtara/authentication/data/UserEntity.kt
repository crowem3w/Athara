package social.ahtara.authentication.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import social.ahtara.authentication.domain.AccountDetails
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.AuthUser
import social.ahtara.authentication.domain.placesFromJson

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
    



    val identifier: String?,
    val displayName: String,
    
    val passwordHash: String?,
    val provider: String,
    val createdAt: Long,
    
    @ColumnInfo(defaultValue = "'PERSONAL'") val accountType: String = AccountType.PERSONAL.name,
    
    val industry: String? = null,
    val phone: String? = null,
    val registrationId: String? = null,
    
    val providerId: String? = null,
    
    val username: String? = null,
    
    val email: String? = null,
    
    val birthday: String? = null,
    
    val about: String? = null,
    val location: String? = null,
    
    val photoPath: String? = null,
    
    val coverPath: String? = null,
    
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    val placesJson: String? = null,
)

internal fun UserEntity.toAuthUser() = AuthUser(
    id = id.toString(),
    
    identifier = username ?: identifier ?: email.orEmpty(),
    username = username,
    email = email,
    birthday = birthday,
    about = about,
    location = location,
    latitude = latitude,
    longitude = longitude,
    places = placesFromJson(placesJson),
    photoPath = photoPath,
    coverPath = coverPath,
    displayName = displayName,
    provider = provider,
    accountType = AccountType.fromName(accountType),
    details = AccountDetails(industry = industry, phone = phone, registrationId = registrationId),
)