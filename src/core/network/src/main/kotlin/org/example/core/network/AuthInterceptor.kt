package org.example.core.network

import okhttp3.Interceptor
import okhttp3.Response


internal class AuthInterceptor(private val tokens: org.example.core.securestorage.TokenStore, private val apiHost: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokens.accessToken
        val skip = token == null ||
            request.url.host != apiHost ||
            request.url.encodedPath.contains(AUTH_PATH) ||
            request.header("Authorization") != null
        if (skip) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
    }

    companion object {
        const val AUTH_PATH = "/v1/auth/"
    }
}