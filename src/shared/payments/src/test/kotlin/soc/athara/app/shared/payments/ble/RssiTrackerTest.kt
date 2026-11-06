package soc.athara.app.shared.payments.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RssiTrackerTest {
    private val floor = -75

    private fun RssiTracker.feed(key: String, vararg dbm: Int, startMs: Long = 0, stepMs: Long = 100) {
        dbm.forEachIndexed { i, v -> add(key, v, startMs + i * stepMs) }
    }

    @Test
    fun medianIgnoresOneWildReading() {
        val t = RssiTracker()
        t.feed("a", -50, -52, -20, -51, -49) // one spike to -20
        val r = t.readings(nowMs = 400).single()
        assertEquals(-50, r.medianDbm)
        assertEquals(5, r.samples)
    }

    @Test
    fun evenCountTakesTheWeakerMiddleValue() {
        assertEquals(-60, RssiTracker.median(listOf(-40, -60, -50, -70)))
    }

    @Test
    fun oldReadingsFallOutOfTheWindow() {
        val t = RssiTracker(windowMs = 1_000)
        t.feed("a", -50, -50, -50, -50) // readings at t = 0, 100, 200, 300
        // At t = 1200 the window starts at 200: the readings at 0 and 100 are gone, 200 and 300 stay.
        assertEquals(2, t.readings(nowMs = 1_200).single().samples)
        assertTrue(t.readings(nowMs = 5_000).isEmpty())
    }

    @Test
    fun singleStrongBeaconIsClearOnceSettled() {
        val t = RssiTracker()
        t.feed("a", -50, -51)
        assertTrue(t.pick(200, floor) is RssiTracker.Pick.Settling)
        t.feed("a", -50, -49, -52, startMs = 200)
        assertTrue(t.pick(500, floor) is RssiTracker.Pick.Clear)
    }

    @Test
    fun twoCloseBeaconsAreAmbiguous() {
        val t = RssiTracker()
        t.feed("a", -50, -50, -50, -50)
        t.feed("b", -53, -53, -53, -53)
        val p = t.pick(400, floor)
        assertTrue(p is RssiTracker.Pick.Ambiguous)
    }

    @Test
    fun aClearlyStrongerBeaconWins() {
        val t = RssiTracker()
        t.feed("near", -45, -46, -45, -44)
        t.feed("far", -70, -71, -70, -69)
        val p = t.pick(400, floor) as RssiTracker.Pick.Clear
        assertEquals("near", p.best.key)
    }

    @Test
    fun beaconsBelowTheFloorAreIgnored() {
        val t = RssiTracker()
        t.feed("far", -90, -91, -90, -92)
        assertTrue(t.pick(400, floor) is RssiTracker.Pick.None)
    }

    @Test
    fun excludedBeaconsAreSkipped() {
        val t = RssiTracker()
        t.feed("refused", -40, -40, -40, -40)
        t.feed("other", -60, -60, -60, -60)
        val p = t.pick(400, floor, exclude = { it == "refused" }) as RssiTracker.Pick.Clear
        assertEquals("other", p.best.key)
    }
}
