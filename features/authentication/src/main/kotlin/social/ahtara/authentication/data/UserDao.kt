package social.ahtara.authentication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface UserDao {
    @Query("SELECT * FROM users")
    suspend fun all(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE identifier = :identifier LIMIT 1")
    suspend fun findByIdentifier(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    
    @Query("SELECT * FROM users WHERE identifier = :key OR username = :key OR email = :key LIMIT 1")
    suspend fun findByAnyId(key: String): UserEntity?

    @Query("SELECT * FROM users WHERE provider = :provider AND providerId = :providerId LIMIT 1")
    suspend fun findByProvider(provider: String, providerId: String): UserEntity?

    
    @Insert
    suspend fun insert(user: UserEntity): Long

    @Query(
        "UPDATE users SET about = :about, location = :location, latitude = :latitude, longitude = :longitude, " +
            "placesJson = :placesJson, photoPath = :photoPath, coverPath = :coverPath WHERE id = :id"
    )
    suspend fun updateProfile(
        id: Long,
        about: String?,
        location: String?,
        latitude: Double?,
        longitude: Double?,
        placesJson: String?,
        photoPath: String?,
        coverPath: String?,
    )

    @Query(
        "UPDATE users SET location = :location, latitude = :latitude, longitude = :longitude, " +
            "placesJson = :placesJson WHERE id = :id"
    )
    suspend fun updatePlaces(id: Long, location: String?, latitude: Double?, longitude: Double?, placesJson: String?)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE users SET passwordHash = :hash WHERE id = :id")
    suspend fun updatePasswordHash(id: Long, hash: String)
}