package soc.athara.app.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.Response


sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>

    




    data class Failure(
        val httpCode: Int?,
        val code: String? = null,
        val existingType: String? = null,
        val network: Boolean = false,
        
        val fieldErrors: Map<String, String> = emptyMap(),
    ) : ApiResult<Nothing> {
        
        val retryable: Boolean get() = network || httpCode == 408 || httpCode == 429 || (httpCode ?: 0) >= 500
    }
}

suspend fun <T> apiCall(block: suspend () -> Response<T>): ApiResult<T> = try {
    val response = block()
    if (response.isSuccessful) {
        val body = response.body()
        if (body != null) ApiResult.Ok(body) else ApiResult.Failure(response.code(), code = "EMPTY_BODY")
    } else {
        val error = response.errorBody()?.string()
            ?.let { runCatching { NetworkJson.decodeFromString<ErrorBody>(it) }.getOrNull() }
        ApiResult.Failure(
            response.code(), error?.code, error?.existingType,
            fieldErrors = error?.fieldErrors?.associate { it.field to it.code }.orEmpty(),
        )
    }
} catch (e: CancellationException) {
    throw e
} catch (e: java.io.IOException) {
    ApiResult.Failure(httpCode = null, network = true)
} catch (e: Exception) {
    ApiResult.Failure(httpCode = null, code = "BAD_RESPONSE") 
}