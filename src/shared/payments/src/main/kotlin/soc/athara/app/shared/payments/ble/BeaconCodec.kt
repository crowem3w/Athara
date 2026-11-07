package soc.athara.app.shared.payments.ble

import java.util.Base64











object BeaconCodec {
    
    const val COMPANY_ID = 0xFFFF

    const val TYPE_MERCHANT: Byte = 0x11
    const val TYPE_PAYER: Byte = 0x12

    private const val HANDLE_BYTES = 6
    private const val NONCE_BYTES = 8
    private const val BEACON_BYTES = 12

    
    sealed interface Heard {
        
        data class Merchant(val handle: String, val nonce: String) : Heard {
            
            val key: String get() = handle
        }

        
        data class Payer(val beaconId: String) : Heard {
            val key: String get() = beaconId
        }
    }

    fun merchantPayload(handle: String, nonce: String): ByteArray? {
        val h = decode(handle, HANDLE_BYTES) ?: return null
        val n = decode(nonce, NONCE_BYTES) ?: return null
        return byteArrayOf(TYPE_MERCHANT) + h + n
    }

    fun payerPayload(beaconId: String): ByteArray? {
        val b = decode(beaconId, BEACON_BYTES) ?: return null
        return byteArrayOf(TYPE_PAYER) + b
    }

    
    fun parse(payload: ByteArray?): Heard? {
        if (payload == null || payload.isEmpty()) return null
        return when (payload[0]) {
            TYPE_MERCHANT -> {
                if (payload.size != 1 + HANDLE_BYTES + NONCE_BYTES) return null
                Heard.Merchant(
                    handle = encode(payload.copyOfRange(1, 1 + HANDLE_BYTES)),
                    nonce = encode(payload.copyOfRange(1 + HANDLE_BYTES, payload.size)),
                )
            }
            TYPE_PAYER -> {
                if (payload.size != 1 + BEACON_BYTES) return null
                Heard.Payer(encode(payload.copyOfRange(1, payload.size)))
            }
            else -> null
        }
    }

    
    internal fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    
    internal fun decode(text: String, size: Int): ByteArray? = try {
        Base64.getUrlDecoder().decode(text).takeIf { it.size == size }
    } catch (e: IllegalArgumentException) {
        null
    }
}
