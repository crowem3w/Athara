package soc.athara.app.shared.payments.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BeaconCodecTest {
    
    private val handle = "AQIDBAUG"
    private val nonce = "AQIDBAUGBwg"
    private val beaconId = "AQIDBAUGBwgJCgsM"

    @Test
    fun merchantPayloadRoundTrips() {
        val bytes = BeaconCodec.merchantPayload(handle, nonce)!!
        assertEquals(15, bytes.size)
        assertEquals(BeaconCodec.TYPE_MERCHANT, bytes[0])
        assertEquals(BeaconCodec.Heard.Merchant(handle, nonce), BeaconCodec.parse(bytes))
    }

    @Test
    fun payerPayloadRoundTrips() {
        val bytes = BeaconCodec.payerPayload(beaconId)!!
        assertEquals(13, bytes.size)
        assertEquals(BeaconCodec.Heard.Payer(beaconId), BeaconCodec.parse(bytes))
    }

    @Test
    fun payloadsFitInALegacyAdvert() {
        
        assert(BeaconCodec.merchantPayload(handle, nonce)!!.size <= 27)
        assert(BeaconCodec.payerPayload(beaconId)!!.size <= 27)
    }

    @Test
    fun wrongSizedIdsAreRefused() {
        assertNull(BeaconCodec.merchantPayload("short", nonce))
        assertNull(BeaconCodec.merchantPayload(handle, "short"))
        assertNull(BeaconCodec.payerPayload(handle))
        assertNull(BeaconCodec.payerPayload("not base64 !!!!!!!"))
    }

    @Test
    fun junkIsNeverParsed() {
        assertNull(BeaconCodec.parse(null))
        assertNull(BeaconCodec.parse(byteArrayOf()))
        assertNull(BeaconCodec.parse(byteArrayOf(0x99.toByte(), 1, 2, 3)))
        
        assertNull(BeaconCodec.parse(byteArrayOf(BeaconCodec.TYPE_MERCHANT, 1, 2)))
        assertNull(BeaconCodec.parse(byteArrayOf(BeaconCodec.TYPE_PAYER) + ByteArray(20)))
        assertNotNull(BeaconCodec.parse(byteArrayOf(BeaconCodec.TYPE_PAYER) + ByteArray(12)))
    }
}
