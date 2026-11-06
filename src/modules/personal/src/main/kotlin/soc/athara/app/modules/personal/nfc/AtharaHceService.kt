package soc.athara.app.modules.personal.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Host Card Emulation: answers a reader's SELECT for the Athara AID with the current payment token.
 * Android starts this service by itself when another phone's reader (see the Business module's `NfcTokenReader`)
 * selects [Aid]. It holds no state of its own; [PaymentTokenHost] owns the token.
 * A read does not mean paid: the business claims the token on the server and the server completes the payment.
 */
internal class AtharaHceService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null || !isSelectForOurAid(commandApdu)) return StatusInsNotSupported
        // Null: no card is open.
        val token = PaymentTokenHost.current() ?: return StatusConditionsNotSatisfied
        return token + StatusOk
    }

    override fun onDeactivated(reason: Int) {
        // Nothing to clean up: the server expires the nonce on its own.
    }

    private fun isSelectForOurAid(apdu: ByteArray): Boolean {
        // CLA INS P1 P2 Lc AID [Le]
        if (apdu.size < 5 + Aid.size) return false
        if (apdu[0] != 0x00.toByte() || apdu[1] != 0xA4.toByte() || apdu[2] != 0x04.toByte()) return false
        if (apdu[4].toInt() and 0xFF != Aid.size) return false
        return Aid.indices.all { apdu[5 + it] == Aid[it] }
    }

    private companion object {
        /** Proprietary AID: 0xF0 followed by "ATHARA". Must match `res/xml/personal_apdu_service.xml` and the reader. */
        val Aid: ByteArray = byteArrayOf(0xF0.toByte(), 0x41, 0x48, 0x54, 0x41, 0x52, 0x41)

        val StatusOk = byteArrayOf(0x90.toByte(), 0x00)
        val StatusConditionsNotSatisfied = byteArrayOf(0x69, 0x85.toByte())
        val StatusInsNotSupported = byteArrayOf(0x6D, 0x00)
    }
}
