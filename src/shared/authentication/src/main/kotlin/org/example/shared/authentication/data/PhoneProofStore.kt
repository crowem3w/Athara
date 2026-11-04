package org.example.shared.authentication.data

import android.os.SystemClock






internal class PhoneProofStore {
    private class Proof(val token: String, val at: Long)

    @Volatile private var proof: Proof? = null

    fun put(token: String) {
        proof = Proof(token, SystemClock.elapsedRealtime())
    }

    
    fun consume(): String? {
        val p = proof ?: return null
        proof = null
        return p.token.takeIf { SystemClock.elapsedRealtime() - p.at < TTL_MS }
    }

    private companion object {
        const val TTL_MS = 10 * 60 * 1000L
    }
}