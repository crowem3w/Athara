package soc.athara.app.shared.payments.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Server contract for tap-to-pay (backend `/v1/payments` and `/v1/wallet`). Internal: callers use [soc.athara.app.shared.payments.Payments]. */
internal interface PaymentsApi {
    @POST("v1/payments/sessions") suspend fun createSession(@Body body: CreateSessionRequest): Response<SessionDto>
    @POST("v1/payments/sessions/claim") suspend fun claim(@Body body: ClaimRequest): Response<SessionDto>
    @POST("v1/payments/sessions/{id}/rotate") suspend fun rotate(@Path("id") id: String): Response<SessionDto>
    @POST("v1/payments/sessions/{id}/accept") suspend fun accept(@Path("id") id: String): Response<SessionDto>
    @POST("v1/payments/sessions/{id}/cancel") suspend fun cancel(@Path("id") id: String): Response<SessionDto>
    @GET("v1/payments/sessions/{id}") suspend fun status(@Path("id") id: String): Response<SessionDto>

    @GET("v1/wallet") suspend fun wallet(): Response<WalletDto>
    @PUT("v1/wallet/test-balance") suspend fun setTestBalance(@Body body: TestBalanceRequest): Response<WalletDto>
}

@Serializable
internal data class CreateSessionRequest(val amountMinor: Long, val currency: String)

@Serializable
internal data class ClaimRequest(val sessionId: String, val nonce: String)

@Serializable
internal data class TestBalanceRequest(val balanceMinor: Long)

@Serializable
internal data class SessionDto(
    val sessionId: String,
    val status: String,
    val amountMinor: Long,
    val currency: String,
    val expiresAt: Long,
    val nonce: String? = null,
    val nonceExpiresAt: Long? = null,
    val payerName: String? = null,
)

@Serializable
internal data class WalletDto(val balanceMinor: Long, val currency: String)
