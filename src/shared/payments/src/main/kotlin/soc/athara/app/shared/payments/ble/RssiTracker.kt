package soc.athara.app.shared.payments.ble








class RssiTracker(private val windowMs: Long = DEFAULT_WINDOW_MS) {

    data class Reading(val key: String, val medianDbm: Int, val samples: Int, val lastSeenMs: Long)

    
    sealed interface Pick {
        
        data object None : Pick
        
        data class Settling(val best: Reading) : Pick
        
        data class Ambiguous(val best: Reading, val second: Reading) : Pick
        data class Clear(val best: Reading) : Pick
    }

    private class Sample(val atMs: Long, val dbm: Int)

    private val samples = HashMap<String, ArrayDeque<Sample>>()

    fun add(key: String, dbm: Int, nowMs: Long) {
        samples.getOrPut(key) { ArrayDeque() }.addLast(Sample(nowMs, dbm))
    }

    fun clear() = samples.clear()

    
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

        
        fun median(values: List<Int>): Int {
            val sorted = values.sorted()
            return sorted[(sorted.size - 1) / 2]
        }
    }
}
