package soc.athara.app.shared.payments

import soc.athara.app.core.network.ApiResult
import soc.athara.app.core.network.Core
import soc.athara.app.core.network.apiCall
import soc.athara.app.shared.payments.data.ClaimRequest
import soc.athara.app.shared.payments.data.CreateSessionRequest
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

    private suspend fun <D, T> call(block: suspend () -> retrofit2.Response<D>, map: (D) -> T): PaymentResult<T> =
        when (val r = apiCall(block)) {
            is ApiResult.Ok -> PaymentResult.Ok(map(r.value))
            is ApiResult.Failure -> PaymentResult.Failed(
                when {
                    r.network -> PaymentError.NETWORK
                    r.httpCode == 401 -> PaymentError.UNAUTHORIZED
                    r.httpCode == 429 -> PaymentError.RATE_LIMITED
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

    private fun WalletDto.toWallet() = Wallet(balanceMinor, currency)
}
