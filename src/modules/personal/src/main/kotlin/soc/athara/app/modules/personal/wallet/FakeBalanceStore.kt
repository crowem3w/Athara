package soc.athara.app.modules.personal.wallet

import android.content.Context

/**
 * TEST MONEY ONLY. Keeps a fake balance per user on this device so the payment flow can be
 * tried before the backend ledger exists. Amounts are minor units (cents).
 *
 * TODO: remove once balances come from the server (`GET /sync`); the server is the only authority.
 */
internal class FakeBalanceStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("personal_fake_wallet", Context.MODE_PRIVATE)

    fun get(userId: String): Long = prefs.getLong(key(userId), DefaultMinor)

    fun set(userId: String, minor: Long) {
        prefs.edit().putLong(key(userId), minor.coerceAtLeast(0L)).apply()
    }

    private fun key(userId: String) = "balance_minor_$userId"

    companion object {
        /** Starting balance for a user who has never set one: 100.00. */
        const val DefaultMinor = 10_000L
    }
}
