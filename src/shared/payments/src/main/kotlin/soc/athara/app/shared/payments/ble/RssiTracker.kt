package soc.athara.app.shared.payments.ble

/**
 * Turns the stream of raw signal-strength readings (RSSI, dBm; closer to 0 = stronger) into one steady number per
 * beacon: the MEDIAN of the readings from the last [windowMs], never a single reading (design section 7).
 * Also decides whether one beacon clearly stands out from the others.
 *
 * No Android types, so it is plain unit-testable. Not thread-safe: call it from one thread (the scan callback posts to main).
 */
class RssiTracker(private val windowMs: Long = DEFAULT_WINDOW_MS) {

    data class Reading(val key: String, val medianDbm: Int, val samples: Int, val lastSeenMs: Long)

    /** What [pick] concluded. */
    sealed interface Pick {
        /** Nothing heard (or nothing loud enough). */
        data object None : Pick
        /** Something is there but there are not enough readings yet. */
        data class Settling(val best: Reading) : Pick
        /** The strongest two are too close to tell apart: ask the person to move closer to ONE phone. */
        data class Ambiguous(val best: Reading, val second: Reading) : Pick
        data class Clear(val best: Reading) : Pick
    }

    private class Sample(val atMs: Long, val dbm: Int)

    private val samples = HashMap<String, ArrayDeque<Sample>>()

    fun add(key: String, dbm: Int, nowMs: Long) {
        samples.getOrPut(key) { ArrayDeque() }.addLast(Sample(nowMs, dbm))
    }

    fun clear() = samples.clear()

    /** Every beacon heard inside the window, strongest first. */
    fun readings(nowMs: Long): List<Reading> {
        val out = ArrayList<Reading>(samples.size)
        val iterator = samples.entries.iterator()
        while (iterator.hasNext()) {
            val (key, queue) = iterator.next()
            while (queue.isNotEmpty() && queue.first().atMs < nowMs - windowMs) queue.removeFirst()
            if (queue.isEmpty()) {
                iterator.remove()
                continue
            }
            out += Reading(key, median(queue.map { it.dbm }), queue.size, queue.last().atMs)
        }
        return out.sortedByDescending { it.medianDbm }
    }

    /**
     * The strongest beacon, if it is a safe pick.
     * [floorDbm]: ignore beacons weaker than this. [minSamples]: readings needed before trusting the median.
     * [marginDb]: how much stronger than the runner-up the best must be.
     * [exclude]: keys to leave out (for example a beacon the server already refused).
     */
    fun pick(
        nowMs: Long,
        floorDbm: Int,
        minSamples: Int = DEFAULT_MIN_SAMPLES,
        marginDb: Int = DEFAULT_MARGIN_DB,
        exclude: (String) -> Boolean = { false },
    ): Pick {
        val heard = readings(nowMs).filter { it.medianDbm >= floorDbm && !exclude(it.key) }
        val best = heard.firstOrNull() ?: return Pick.None
        if (best.samples < minSamples) return Pick.Settling(best)
        val second = heard.getOrNull(1)
        if (second != null && best.medianDbm - second.medianDbm < marginDb) return Pick.Ambiguous(best, second)
        return Pick.Clear(best)
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 1_200L
        const val DEFAULT_MIN_SAMPLES = 4
        const val DEFAULT_MARGIN_DB = 6

        /** Median of a non-empty list; the lower middle value for an even count (the cautious one: weaker signal). */
        fun median(values: List<Int>): Int {
            val sorted = values.sorted()
            return sorted[(sorted.size - 1) / 2]
        }
    }
}
