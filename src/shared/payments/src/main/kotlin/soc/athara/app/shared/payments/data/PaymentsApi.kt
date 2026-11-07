package soc.athara.app.shared.payments.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Server contract for wallet and Bluetooth payments (backend `/v1/wallet` and `/v1/proximity`). Internal: callers use [soc.athara.app.shared.payments.Payments]. */
internal interface PaymentsApi {
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
internal data class TestBalanceRequest(val balanceMinor: Long)

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
