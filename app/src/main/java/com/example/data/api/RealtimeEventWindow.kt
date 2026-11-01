package com.example.data.api

/** Bounded session-local duplicate suppression; PostgreSQL remains the message source of truth. */
class RealtimeEventWindow(private val capacity: Int = 2048, private val ttlMillis: Long = 120000L) {
    private val seen = LinkedHashMap<String, Long>()
    @Synchronized fun claim(key: String, nowMillis: Long): Boolean {
        val iterator = seen.entries.iterator()
        while (iterator.hasNext()) if (nowMillis - iterator.next().value >= ttlMillis) iterator.remove()
        if (seen.containsKey(key)) return false
        while (seen.size >= capacity) seen.remove(seen.keys.first())
        seen[key] = nowMillis
        return true
    }
    @Synchronized fun clear() { seen.clear() }
}
