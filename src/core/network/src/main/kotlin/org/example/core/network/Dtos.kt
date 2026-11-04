package org.example.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject


val NetworkJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}


@Serializable
data class FieldIssue(val field: String, val code: String, val message: String? = null)

@Serializable
data class ErrorBody(
    val code: String? = null,
    val message: String? = null,
    val existingType: String? = null,
    val fieldErrors: List<FieldIssue> = emptyList(),
)




@Serializable
data class RecordDto(
    val serverId: String,
    val clientId: String? = null,
    val version: Long,
    val deleted: Boolean = false,
    val data: JsonObject = JsonObject(emptyMap()),
)


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


@Serializable
data class MutationResultDto(val clientId: String, val status: String, val record: RecordDto? = null)

@Serializable
data class PushResponse(val results: List<MutationResultDto>)

@Serializable
data class PullResponse(val records: List<RecordDto>, val cursor: String? = null, val hasMore: Boolean = false)



@Serializable
internal data class RefreshRequest(val refreshToken: String)


@Serializable
internal data class RefreshUser(val id: String)

@Serializable
internal data class RefreshResponse(val accessToken: String, val refreshToken: String, val user: RefreshUser? = null)