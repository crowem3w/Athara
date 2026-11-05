package org.example.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.example.core.securestorage.TokenStore








internal class TokenAuthenticator(
    private val tokens: TokenStore,
    private val refreshApi: TokenRefreshApi, 
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains(AuthInterceptor.AUTH_PATH)) return null
        if (attempts(response) >= 2) return null
        return synchronized(this) { refreshLocked(response) }
    }

    private fun refreshLocked(response: Response): Request? {
        val current = tokens.accessToken ?: return null 
        val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (sent != null && sent != current) return retry(response, current) 

        val refresh = tokens.refreshToken ?: return null
        val userId = tokens.userId.orEmpty()
        return when (val result = runBlocking { apiCall { refreshApi.refresh(RefreshRequest(refresh)) } }) {
            is ApiResult.Ok -> {
                val body = result.value
                tokens.save(TokenStore.Tokens(body.accessToken, body.refreshToken, body.user?.id ?: userId))
                retry(response, body.accessToken)
            }
            is ApiResult.Failure -> {
                if (result.httpCode in SESSION_ENDED) tokens.clear() // not 408/429: those are temporary, keep the session 
                null
            }
        }
    }

    private companion object {
        val SESSION_ENDED = setOf(400, 401, 403)
    }

    private fun retry(response: Response, token: String): Request =
        response.request.newBuilder().header("Authorization", "Bearer $token").build()

    private fun attempts(response: Response): Int = generateSequence(response) { it.priorResponse }.count()
}