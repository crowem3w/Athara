package soc.athara.app.modules.business.reader

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Handler
import android.os.Looper
import java.io.IOException

internal enum class NfcAvailability { UNSUPPORTED, DISABLED, ENABLED }

internal fun nfcAvailability(context: Context): NfcAvailability {
    val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcAvailability.UNSUPPORTED
    return if (adapter.isEnabled) NfcAvailability.ENABLED else NfcAvailability.DISABLED
}

/** The receiver's Host Card Emulation service must answer this AID (plan section 5). */
internal object NfcProtocol {
    /** Proprietary AID: 0xF0 followed by "AHTARA". */
    val AID: ByteArray = byteArrayOf(0xF0.toByte(), 0x41, 0x48, 0x54, 0x41, 0x52, 0x41)

    /** ISO 7816 SELECT by name: CLA INS P1 P2 Lc AID Le. */
    val SELECT: ByteArray = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, AID.size.toByte()) + AID + byteArrayOf(0x00)

    const val TRANSCEIVE_TIMEOUT_MS = 3000
}

/**
 * Reader mode (payer side in the plan, used here by the business to collect): sends SELECT for our AID and returns
 * the text the other phone answers with. [onResult] runs on the main thread with null when the read failed.
 */
internal class NfcTokenReader(
    private val activity: Activity,
    private val onResult: (String?) -> Unit,
) : NfcAdapter.ReaderCallback {

    private val main = Handler(Looper.getMainLooper())

    /** False when NFC is missing or off. Safe to call again. */
    fun enable(): Boolean {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return false
        if (!adapter.isEnabled) return false
        adapter.enableReaderMode(
            activity,
            this,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null,
        )
        return true
    }

    fun disable() {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            adapter.disableReaderMode(activity)
        } catch (e: IllegalStateException) {
            // Activity already going away.
        }
    }

    // Binder thread.
    override fun onTagDiscovered(tag: Tag) {
        val text = read(tag)
        main.post { onResult(text) }
    }

    private fun read(tag: Tag): String? {
        val iso = IsoDep.get(tag) ?: return null
        return try {
            iso.connect()
            iso.timeout = NfcProtocol.TRANSCEIVE_TIMEOUT_MS
            val response = iso.transceive(NfcProtocol.SELECT)
            val ok = response.size >= 2 &&
                response[response.size - 2] == 0x90.toByte() &&
                response[response.size - 1] == 0x00.toByte()
            if (ok) String(response, 0, response.size - 2, Charsets.UTF_8) else null
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } finally {
            try {
                iso.close()
            } catch (e: IOException) {
                // Nothing to do.
            }
        }
    }
}
