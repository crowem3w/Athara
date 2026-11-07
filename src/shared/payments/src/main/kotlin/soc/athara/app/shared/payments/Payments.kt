package soc.athara.app.shared.payments

import soc.athara.app.core.network.ApiResult
import soc.athara.app.core.network.Core
import soc.athara.app.core.network.apiCall
import soc.athara.app.shared.payments.data.ClaimIntentRequest
import soc.athara.app.shared.payments.data.CreateIntentRequest
import soc.athara.app.shared.payments.data.IntentDto
import soc.athara.app.shared.payments.data.PaymentsApi
import soc.athara.app.shared.payments.data.TestBalanceRequest
import soc.athara.app.shared.payments.data.WalletDto

enum class PaymentStatus { CREATED, CLAIMED, COMPLETED, CANCELLED, EXPIRED }

data class Wallet(val balanceMinor: Long, val currency: String)


data class MerchantBeaconInfo(val handle: String, val nonce: String, val ttlSeconds: Int)


enum class Proximity {
    
    NEAR,
    
    BORDERLINE,
}


data class ProximityIntent(
    val intentId: String,
    val status: PaymentStatus,
    val amountMinor: Long,
    val currency: String,
    val beaconId: String? = null,
    
    val payerName: String? = null,
    
    val merchantName: String? = null,
    
    val proximity: Proximity? = null,
    
    val reducedMode: Boolean = false,
)

enum class PaymentError {
    
    INSUFFICIENT_FUNDS,
    
    INVALID_TOKEN,
    
    SESSION_NOT_OPEN,
    
    NOT_ALLOWED,
    NOT_FOUND,
    
    UNAUTHORIZED,
    RATE_LIMITED,
    
    TOO_FAR,
    
    AMOUNT_OVER_LIMIT,
    
    DAILY_LIMIT,
    
    NETWORK,
    OTHER,
}

sealed interface PaymentResult<out T> {
    data class Ok<T>(val value: T) : PaymentResult<T>
    data class Failed(val error: PaymentError) : PaymentResult<Nothing>
}








object Payments {
    private val api: PaymentsApi by lazy { Core.create(PaymentsApi::class.java) }

    suspend fun wallet(): PaymentResult<Wallet> = call({ api.wallet() }) { it.toWallet() }

    
    suspend fun setTestBalance(minor: Long): PaymentResult<Wallet> =
        call({ api.setTestBalance(TestBalanceRequest(minor)) }) { it.toWallet() }

    

    
    suspend fun proximityBeacon(): PaymentResult<MerchantBeaconInfo> =
        call({ api.proximityBeacon() }) { MerchantBeaconInfo(it.handle, it.nonce, it.ttlSeconds) }

    



    suspend fun createIntent(
        amountMinor: Long,
        merchantHandle: String,
        merchantNonce: String,
        rssi: Int,
        reducedMode: Boolean,
        currency: String = "PHP",
    ): PaymentResult<ProximityIntent> =
        call({ api.createIntent(CreateIntentRequest(amountMinor, currency, merchantHandle, merchantNonce, rssi, reducedMode)) }) { it.toIntent() }

    
    suspend fun claimIntent(beaconId: String, rssi: Int): PaymentResult<ProximityIntent> =
        call({ api.claimIntent(ClaimIntentRequest(beaconId = beaconId, rssi = rssi)) }) { it.toIntent() }

    
    suspend fun claimIntentById(intentId: String): PaymentResult<ProximityIntent> =
        call({ api.claimIntent(ClaimIntentRequest(intentId = intentId)) }) { it.toIntent() }

    
    suspend fun proximityInbox(): PaymentResult<List<ProximityIntent>> =
        call({ api.proximityInbox() }) { box -> box.requests.map { it.toIntent() } }

    suspend fun acceptIntent(intentId: String): PaymentResult<ProximityIntent> = call({ api.acceptIntent(intentId) }) { it.toIntent() }

    suspend fun declineIntent(intentId: String): PaymentResult<ProximityIntent> = call({ api.declineIntent(intentId) }) { it.toIntent() }

    suspend fun cancelIntent(intentId: String): PaymentResult<ProximityIntent> = call({ api.cancelIntent(intentId) }) { it.toIntent() }

    suspend fun intentStatus(intentId: String): PaymentResult<ProximityIntent> = call({ api.intentStatus(intentId) }) { it.toIntent() }

    private suspend fun <D, T> call(block: suspend () -> retrofit2.Response<D>, map: (D) -> T): PaymentResult<T> =
        when (val r = apiCall(block)) {
            is ApiResult.Ok -> PaymentResult.Ok(map(r.value))
            is ApiResult.Failure -> PaymentResult.Failed(
                when {
                    r.network -> PaymentError.NETWORK
                    r.httpCode == 401 -> PaymentError.UNAUTHORIZED
                    r.httpCode == 429 -> PaymentError.RATE_LIMITED
                    r.code == "TOO_FAR" -> PaymentError.TOO_FAR
                    r.code == "AMOUNT_OVER_LIMIT" -> PaymentError.AMOUNT_OVER_LIMIT
                    r.code == "DAILY_LIMIT" -> PaymentError.DAILY_LIMIT
                    r.code == "INSUFFICIENT_FUNDS" -> PaymentError.INSUFFICIENT_FUNDS
                    r.code == "INVALID_TOKEN" -> PaymentError.INVALID_TOKEN
                    r.code == "SESSION_NOT_OPEN" || r.code == "ALREADY_COMPLETED" -> PaymentError.SESSION_NOT_OPEN
                    r.code == "NOT_ALLOWED" -> PaymentError.NOT_ALLOWED
                    r.code == "NOT_FOUND" -> PaymentError.NOT_FOUND
                    else -> PaymentError.OTHER
                },
            )
        }

    private fun IntentDto.toIntent() = ProximityIntent(
        intentId = intentId,
        status = runCatching { PaymentStatus.valueOf(status) }.getOrDefault(PaymentStatus.EXPIRED),
        amountMinor = amountMinor,
        currency = currency,
        beaconId = beaconId,
        payerName = payerName,
        merchantName = merchantName,
        proximity = proximity?.let { p -> runCatching { Proximity.valueOf(p) }.getOrNull() },
        reducedMode = reducedMode,
    )

    private fun WalletDto.toWallet() = Wallet(balanceMinor, currency)
}
