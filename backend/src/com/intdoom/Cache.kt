// Cache.kt
// ---------------------------------------------------------------------------
// The cache is a very simple "box" that remembers content so we don't scrape
// the same website over and over. It has two layers:
//
//   - memory  : fast, but disappears when the server stops
//   - disk    : a JSON file, so content survives a server restart
//
// Shape of the data:  { topic: { items: [...], updatedAt: 1234567890 } }
// ---------------------------------------------------------------------------

package com.intdoom

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File

// One topic's remembered content.
@Serializable
data class CacheEntry(val items: List<Item>, val updatedAt: Long)

object Cache {

    // The cache file lives in backend/.cache/ (run.bat starts the server from
    // the backend folder, so this relative path works).
    private val file = File(".cache/content.json")

    // topic -> entry. Loaded from disk when the server starts.
    private val store: MutableMap<String, CacheEntry> = loadFromDisk()

    private fun loadFromDisk(): MutableMap<String, CacheEntry> =
        try {
            json.decodeFromString<Map<String, CacheEntry>>(file.readText()).toMutableMap()
        } catch (error: Exception) {
            mutableMapOf() // first run, or the file was deleted. Start empty.
        }

    private fun saveToDisk() {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(store))
    }

    // Get one topic's cached entry, or null if we have never scraped it.
    @Synchronized
    fun get(topic: String): CacheEntry? = store[topic]

    // Save fresh content for a topic.
    @Synchronized
    fun set(topic: String, items: List<Item>) {
        store[topic] = CacheEntry(items, System.currentTimeMillis())
        saveToDisk()
    }

    // Is the cached content still young enough to use?
    @Synchronized
    fun isFresh(topic: String, maxAgeMs: Long): Boolean {
        val entry = store[topic] ?: return false
        if (entry.items.isEmpty()) return false
        return System.currentTimeMillis() - entry.updatedAt < maxAgeMs
    }

    // Small summary used by the /api/status endpoint.
    @Synchronized
    fun meta(): Map<String, TopicMeta> =
        store.mapValues { (_, entry) -> TopicMeta(entry.items.size, entry.updatedAt) }
}
