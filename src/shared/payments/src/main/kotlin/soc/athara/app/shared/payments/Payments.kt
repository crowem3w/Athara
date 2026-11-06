package soc.athara.app.shared.payments

import soc.athara.app.core.network.ApiResult
import soc.athara.app.core.network.Core
import soc.athara.app.core.network.apiCall
import soc.athara.app.shared.payments.data.ClaimIntentRequest
import soc.athara.app.shared.payments.data.ClaimRequest
import soc.athara.app.shared.payments.data.CreateIntentRequest
import soc.athara.app.shared.payments.data.CreateSessionRequest
import soc.athara.app.shared.payments.data.IntentDto
import soc.athara.app.shared.payments.data.PaymentsApi
import soc.athara.app.shared.payments.data.SessionDto
import soc.athara.app.shared.payments.data.TestBalanceRequest
import soc.athara.app.shared.payments.data.WalletDto

enum class PaymentStatus { CREATED, CLAIMED, COMPLETED, CANCELLED, EXPIRED;
    val isFinal: Boolean get() = this == COMPLETED || this == CANCELLED || this == EXPIRED
}

/** A tap-to-pay request. [nonce] is only present right after the payer creates or rotates it. */
data class PaymentSession(
    val sessionId: String,
    val status: PaymentStatus,
    val amountMinor: Long,
    val currency: String,
    val nonce: String? = null,
    /** Who is paying; only filled in for the business that claimed the session. */
    val payerName: String? = null,
)

data class Wallet(val balanceMinor: Long, val currency: String)

/** The business's rotating Bluetooth beacon: broadcast [handle] and [nonce] (see `ble.BeaconCodec`); ask again after [ttlSeconds]. */
data class MerchantBeaconInfo(val handle: String, val nonce: String, val ttlSeconds: Int)

/** How clean the signal check was once the business claimed the payment. */
enum class Proximity {
    /** Both phones measured a strong signal. */
    NEAR,
    /** The check was not clean (weak or disagreeing readings, or the payer phone could not broadcast): confirm the customer is right there. */
    BORDERLINE,
}

/** A Bluetooth payment. [beaconId] is only present right after the payer creates it. */
data class ProximityIntent(
    val intentId: String,
    val status: PaymentStatus,
    val amountMinor: Long,
    val currency: String,
    val beaconId: String? = null,
    /** Only filled in for the business. */
    val payerName: String? = null,
    /** Only filled in for the payer. */
    val merchantName: String? = null,
    /** Set once claimed. */
    val proximity: Proximity? = null,
    /** The payer phone could not broadcast, so the business must confirm in person. */
    val reducedMode: Boolean = false,
)

enum class PaymentError {
    /** Payer cannot afford it. */
    INSUFFICIENT_FUNDS,
    /** The token read from the card is wrong or too old. */
    INVALID_TOKEN,
    /** Already claimed, finished, cancelled or expired. */
    SESSION_NOT_OPEN,
    /** This account type may not do that (for example a personal account trying to collect). */
    NOT_ALLOWED,
    NOT_FOUND,
    /** Not signed in, or the session could not be refreshed. */
    UNAUTHORIZED,
    RATE_LIMITED,
    /** Bluetooth: the signal says the phones are not close enough. Move closer and try again. */
    TOO_FAR,
    /** Bluetooth payments have lower limits than tap: this amount is over the per-payment limit. */
    AMOUNT_OVER_LIMIT,
    /** Bluetooth payments have a daily limit and it is reached. */
    DAILY_LIMIT,
    /** No connection or the server could not be reached. */
    NETWORK,
    OTHER,
}

sealed interface PaymentResult<out T> {
    data class Ok<T>(val value: T) : PaymentResult<T>
    data class Failed(val error: PaymentError) : PaymentResult<Nothing>
}

/**
 * Client for the server's tap-to-pay API. Money only moves on the server: the app asks, the server decides.
 * All calls go through the signed-in [Core] HTTP client (token refresh included).
 *
 * Payer (personal): [createSession] -> show the card, [rotate] every few seconds, poll [status] -> done.
 * Receiver (business): read the token, [claim] it, show the amount, then [accept] or [cancel].
 */
object Payments {
    private val api: PaymentsApi by lazy { Core.create(PaymentsApi::class.java) }

    suspend fun wallet(): PaymentResult<Wallet> = call({ api.wallet() }) { it.toWallet() }

    /** TEST MONEY: only works while the server has TEST_BALANCE_ENABLED on (it is off in production). */
    suspend fun setTestBalance(minor: Long): PaymentResult<Wallet> =
        call({ api.setTestBalance(TestBalanceRequest(minor)) }) { it.toWallet() }

    suspend fun createSession(amountMinor: Long, currency: String = "PHP"): PaymentResult<PaymentSession> =
        call({ api.createSession(CreateSessionRequest(amountMinor, currency)) }) { it.toSession() }

    suspend fun rotate(sessionId: String): PaymentResult<PaymentSession> = call({ api.rotate(sessionId) }) { it.toSession() }

    suspend fun status(sessionId: String): PaymentResult<PaymentSession> = call({ api.status(sessionId) }) { it.toSession() }

    suspend fun cancel(sessionId: String): PaymentResult<PaymentSession> = call({ api.cancel(sessionId) }) { it.toSession() }

    suspend fun claim(sessionId: String, nonce: String): PaymentResult<PaymentSession> =
        call({ api.claim(ClaimRequest(sessionId, nonce)) }) { it.toSession() }

    suspend fun accept(sessionId: String): PaymentResult<PaymentSession> = call({ api.accept(sessionId) }) { it.toSession() }

    // ---- Bluetooth proximity payments ----

    /** Business: the next beacon nonce to broadcast. Call again before it expires while scanning. */
    suspend fun proximityBeacon(): PaymentResult<MerchantBeaconInfo> =
        call({ api.proximityBeacon() }) { MerchantBeaconInfo(it.handle, it.nonce, it.ttlSeconds) }

    /**
     * Payer: start a payment to the business whose beacon ([merchantHandle], [merchantNonce]) was heard.
     * [rssi] is the median signal strength measured; [reducedMode] is true when this phone cannot broadcast.
     */
    suspend fun createIntent(
        amountMinor: Long,
        merchantHandle: String,
        merchantNonce: String,
        rssi: Int,
        reducedMode: Boolean,
        currency: String = "PHP",
    ): PaymentResult<ProximityIntent> =
        call({ api.createIntent(CreateIntentRequest(amountMinor, currency, merchantHandle, merchantNonce, rssi, reducedMode)) }) { it.toIntent() }

    /** Business: claim the payer beacon it heard, with the median signal strength it measured. */
    suspend fun claimIntent(beaconId: String, rssi: Int): PaymentResult<ProximityIntent> =
        call({ api.claimIntent(ClaimIntentRequest(beaconId = beaconId, rssi = rssi)) }) { it.toIntent() }

    /** Business, reduced mode only: claim a request from [proximityInbox]. */
    suspend fun claimIntentById(intentId: String): PaymentResult<ProximityIntent> =
        call({ api.claimIntent(ClaimIntentRequest(intentId = intentId)) }) { it.toIntent() }

    /** Business: requests waiting from payer phones that could not broadcast. */
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

    private fun SessionDto.toSession() = PaymentSession(
        sessionId = sessionId,
        status = runCatching { PaymentStatus.valueOf(status) }.getOrDefault(PaymentStatus.EXPIRED),
        amountMinor = amountMinor,
        currency = currency,
        nonce = nonce,
        payerName = payerName,
    )

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
