package org.example.shared.authentication.data

import android.os.SystemClock

/**
 * In-memory holder for the proof the backend issues after a mobile number is verified by OTP.
 * The repository attaches it (once) to the next sign-up / password-reset request, which the backend
 * must check against the number or account concerned. Never persisted.
 */
internal class PhoneProofStore {
    private class Proof(val token: String, val at: Long)

    @Volatile private var proof: Proof? = null

    fun put(token: String) {
        proof = Proof(token, SystemClock.elapsedRealtime())
    }

    /** Returns the proof and forgets it; null if there is none or it is older than 10 minutes. */
    fun consume(): String? {
        val p = proof ?: return null
        proof = null
        return p.token.takeIf { SystemClock.elapsedRealtime() - p.at < TTL_MS }
    }

    private companion object {
        const val TTL_MS = 10 * 60 * 1000L
    }
}
