package org.example.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Shared JSON configuration: tolerant of new server fields, explicit about defaults, omits nulls. */
val NetworkJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/** Error body for every 4xx: `code` is a stable machine-readable string (see docs/API_CONTRACT.md). */
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

// ---- Token refresh (used only by TokenAuthenticator) ----------------------------------------

@Serializable
internal data class RefreshRequest(val refreshToken: String)

/** The refresh response carries a full user object; only its id matters here (unknown keys are ignored). */
@Serializable
internal data class RefreshUser(val id: String)

@Serializable
internal data class RefreshResponse(val accessToken: String, val refreshToken: String, val user: RefreshUser? = null)
