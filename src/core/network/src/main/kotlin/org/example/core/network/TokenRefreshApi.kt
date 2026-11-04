package org.example.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


internal interface TokenRefreshApi {
    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<RefreshResponse>
}