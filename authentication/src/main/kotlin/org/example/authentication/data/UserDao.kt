package org.example.authentication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
internal interface UserDao {
    @Query("SELECT * FROM users WHERE identifier = :identifier LIMIT 1")
    suspend fun findByIdentifier(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    /** Sign-in / password reset: [key] is a username, an email or an E.164 mobile number (formats never overlap). */
    @Query("SELECT * FROM users WHERE identifier = :key OR username = :key OR email = :key LIMIT 1")
    suspend fun findByAnyId(key: String): UserEntity?

    @Query("SELECT * FROM users WHERE provider = :provider AND providerId = :providerId LIMIT 1")
    suspend fun findByProvider(provider: String, providerId: String): UserEntity?

    /** Throws SQLiteConstraintException if the identifier already exists. */
    @Insert
    suspend fun insert(user: UserEntity): Long

    @Query("UPDATE users SET about = :about, location = :location, photoPath = :photoPath, coverPath = :coverPath WHERE id = :id")
    suspend fun updateProfile(id: Long, about: String?, location: String?, photoPath: String?, coverPath: String?)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE users SET passwordHash = :hash WHERE id = :id")
    suspend fun updatePasswordHash(id: Long, hash: String)
}
