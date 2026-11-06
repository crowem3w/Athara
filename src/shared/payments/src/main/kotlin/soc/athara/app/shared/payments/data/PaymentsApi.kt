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

    // Bluetooth proximity payments (backend `/v1/proximity`).
    @POST("v1/proximity/beacon") suspend fun proximityBeacon(): Response<BeaconDto>
    @POST("v1/proximity/intents") suspend fun createIntent(@Body body: CreateIntentRequest): Response<IntentDto>
    @POST("v1/proximity/intents/claim") suspend fun claimIntent(@Body body: ClaimIntentRequest): Response<IntentDto>
    @GET("v1/proximity/inbox") suspend fun proximityInbox(): Response<InboxDto>
    @POST("v1/proximity/intents/{id}/accept") suspend fun acceptIntent(@Path("id") id: String): Response<IntentDto>
    @POST("v1/proximity/intents/{id}/decline") suspend fun declineIntent(@Path("id") id: String): Response<IntentDto>
    @POST("v1/proximity/intents/{id}/cancel") suspend fun cancelIntent(@Path("id") id: String): Response<IntentDto>
    @GET("v1/proximity/intents/{id}") suspend fun intentStatus(@Path("id") id: String): Response<IntentDto>

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

@Serializable
internal data class BeaconDto(val handle: String, val nonce: String, val expiresAt: Long, val ttlSeconds: Int)

@Serializable
internal data class CreateIntentRequest(
    val amountMinor: Long,
    val currency: String,
    val merchantHandle: String,
    val merchantNonce: String,
    val rssi: Int,
    val reducedMode: Boolean,
)

@Serializable
internal data class ClaimIntentRequest(val beaconId: String? = null, val rssi: Int? = null, val intentId: String? = null)

@Serializable
internal data class IntentDto(
    val intentId: String,
    val status: String,
    val amountMinor: Long,
    val currency: String,
    val expiresAt: Long,
    val beaconId: String? = null,
    val payerName: String? = null,
    val merchantName: String? = null,
    val proximity: String? = null,
    val reducedMode: Boolean = false,
)

@Serializable
internal data class InboxDto(val requests: List<IntentDto> = emptyList())
