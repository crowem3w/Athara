package org.example.core.net

import kotlinx.coroutines.CancellationException
import retrofit2.Response

/** Outcome of one API call, with no exceptions to catch at call sites. */
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>

    /**
     * @property httpCode null when no HTTP response was received.
     * @property network true for I/O problems (offline, timeout, DNS): retrying later may work.
     * @property code the server's stable error code (see [ErrorBody]), when it sent one.
     */
    data class Failure(
        val httpCode: Int?,
        val code: String? = null,
        val existingType: String? = null,
        val network: Boolean = false,
    ) : ApiResult<Nothing> {
        /** Worth retrying later: offline, 408, 429 or any 5xx. */
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
            ?.let { runCatching { CoreJson.decodeFromString<ErrorBody>(it) }.getOrNull() }
        ApiResult.Failure(response.code(), error?.code, error?.existingType)
    }
} catch (e: CancellationException) {
    throw e
} catch (e: java.io.IOException) {
    ApiResult.Failure(httpCode = null, network = true)
} catch (e: Exception) {
    ApiResult.Failure(httpCode = null, code = "BAD_RESPONSE") // e.g. a body that does not match the contract
}
