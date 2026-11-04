package org.example.core.identityapi

/**
 * Hook for SMS one-time-password verification. The default [NoVerification] accepts every
 * number, so sign-up works offline today. To add real SMS later, implement this against your
 * provider (Firebase Phone Auth, Twilio Verify, ...) and register it with
 * [org.example.shared.authentication.Authentication.installPhoneVerifier].
 */
interface PhoneVerifier {
    /** Sends a code to [e164] (e.g. +639171234567). */
    suspend fun sendCode(e164: String): Boolean

    /** True if [code] is valid for [e164]. */
    suspend fun verify(e164: String, code: String): Boolean
}

object NoVerification : PhoneVerifier {
    override suspend fun sendCode(e164: String) = true
    override suspend fun verify(e164: String, code: String) = true
}
