package org.example.core.net

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.example.core.secure.TokenStore

/**
 * Called by OkHttp on a 401. Exchanges the refresh token for a new pair (single-flight: concurrent
 * 401s wait on the lock and reuse the token the first one obtained) and retries the request once.
 * If the server rejects the refresh token the session is over: tokens are cleared and
 * [TokenStore.signedIn] turns false so the app returns to the sign-in screen. Network errors during
 * refresh keep the tokens, so a flaky connection never signs the user out.
 */
internal class TokenAuthenticator(
    private val tokens: TokenStore,
    private val refreshApi: BackendApi, // built on a client WITHOUT this authenticator
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains(AuthInterceptor.AUTH_PATH)) return null
        if (attempts(response) >= 2) return null
        return synchronized(this) { refreshLocked(response) }
    }

    private fun refreshLocked(response: Response): Request? {
        val current = tokens.accessToken ?: return null // already signed out
        val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (sent != null && sent != current) return retry(response, current) // another call already refreshed

        val refresh = tokens.refreshToken ?: return null
        val userId = tokens.userId.orEmpty()
        return when (val result = runBlocking { apiCall { refreshApi.refresh(RefreshRequest(refresh)) } }) {
            is ApiResult.Ok -> {
                val body = result.value
                tokens.save(TokenStore.Tokens(body.accessToken, body.refreshToken, body.user?.id ?: userId))
                retry(response, body.accessToken)
            }
            is ApiResult.Failure -> {
                if (result.httpCode in 400..499) tokens.clear() // refresh token rejected: session over
                null
            }
        }
    }

    private fun retry(response: Response, token: String): Request =
        response.request.newBuilder().header("Authorization", "Bearer $token").build()

    private fun attempts(response: Response): Int = generateSequence(response) { it.priorResponse }.count()
}
