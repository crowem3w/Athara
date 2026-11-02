package org.example.core.net

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Shared JSON configuration: tolerant of new server fields, explicit about defaults, omits nulls. */
val CoreJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

// ---- Auth ------------------------------------------------------------------------------------

@Serializable
data class DetailsDto(val industry: String? = null, val phone: String? = null, val registrationId: String? = null)

@Serializable
data class SignUpRequest(
    val name: String,
    val identifier: String? = null,
    val password: String,
    val accountType: String,
    val details: DetailsDto? = null,
    val username: String? = null,
    val email: String? = null,
    val birthday: String? = null,
    val phoneVerificationToken: String? = null,
)

@Serializable
data class SignInRequest(val identifier: String, val password: String)

@Serializable
data class SocialLoginRequest(val provider: String, val idToken: String)

@Serializable
data class SocialSignUpRequest(
    val provider: String,
    val idToken: String,
    val identifier: String,
    val accountType: String,
    val phoneVerificationToken: String? = null,
)

@Serializable
data class ResetPasswordRequest(val identifier: String, val newPassword: String, val phoneVerificationToken: String? = null)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class SendCodeRequest(val phone: String)

@Serializable
data class VerifyCodeRequest(val phone: String, val code: String)

@Serializable
data class VerifyCodeResponse(val verificationToken: String)

@Serializable
data class ProfileUpdateRequest(val about: String? = null, val location: String? = null)

@Serializable
data class UserDto(
    val id: String,
    val identifier: String? = null,
    val displayName: String,
    val provider: String = "password",
    val accountType: String = "PERSONAL",
    val industry: String? = null,
    val phone: String? = null,
    val registrationId: String? = null,
    val username: String? = null,
    val email: String? = null,
    val birthday: String? = null,
    val about: String? = null,
    val location: String? = null,
    val photoUrl: String? = null,
    val coverUrl: String? = null,
)

/** [user] is optional on refresh responses. */
@Serializable
data class TokenResponse(val accessToken: String, val refreshToken: String, val user: UserDto? = null)

/** Error body for every 4xx: `code` is a stable machine-readable string (see API_CONTRACT.md). */
@Serializable
data class ErrorBody(val code: String? = null, val message: String? = null, val existingType: String? = null)

// ---- Sync ------------------------------------------------------------------------------------

/** One record as the server knows it. [data] holds the resource-specific fields. */
@Serializable
data class RecordDto(
    val serverId: String,
    val clientId: String? = null,
    val version: Long,
    val deleted: Boolean = false,
    val data: JsonObject = JsonObject(emptyMap()),
)

/** A local change waiting to be sent. [op] is "upsert" or "delete". */
@Serializable
data class MutationDto(
    val clientId: String,
    val serverId: String? = null,
    val baseVersion: Long = 0,
    val op: String,
    val data: JsonObject? = null,
)

@Serializable
data class PushRequest(val mutations: List<MutationDto>)

/** [status]: "applied" | "conflict" | "rejected" | "gone". [record] is the server's canonical state, if any. */
@Serializable
data class MutationResultDto(val clientId: String, val status: String, val record: RecordDto? = null)

@Serializable
data class PushResponse(val results: List<MutationResultDto>)

@Serializable
data class PullResponse(val records: List<RecordDto>, val cursor: String? = null, val hasMore: Boolean = false)
