package soc.athara.app.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface SyncApi {
    @POST("v1/sync/{resource}/push")
    suspend fun push(@Path("resource") resource: String, @Body body: PushRequest): Response<PushResponse>

    @GET("v1/sync/{resource}")
    suspend fun pull(
        @Path("resource") resource: String,
        @Query("since") since: String?,
        @Query("limit") limit: Int,
    ): Response<PullResponse>
}