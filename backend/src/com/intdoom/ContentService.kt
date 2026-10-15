// ContentService.kt
// ---------------------------------------------------------------------------
// This sits between the routes and the scraper/cache.
// It answers one question: "give me content for a topic, as fast as possible".
//
// Rules:
//   1. If the cache is fresh -> return it instantly.
//   2. Otherwise -> scrape the web, save it, then return it.
//   3. If scraping fails -> return old cached content if we have any.
// ---------------------------------------------------------------------------

package com.intdoom

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

val MAX_AGE_MS: Long = CACHE_MINUTES * 60 * 1000L

fun listTopics(): List<String> = listOf("all") + TOPICS

fun status(): StatusResponse = StatusResponse(CACHE_MINUTES, Cache.meta())

// Scrape a topic and store the result. Used by the scheduler and by ?refresh=1.
suspend fun refreshTopic(topic: String): List<Item> {
    println("[content] scraping \"$topic\"...")
    val items = scrapeTopic(topic)

    if (items.isNotEmpty()) {
        Cache.set(topic, items)
        println("[content] saved ${items.size} items for \"$topic\".")
    } else {
        println("[content] no items found for \"$topic\".")
    }
    return items
}

// The main function used by the API route.
suspend fun getTopicContent(topic: String, force: Boolean = false): ContentResponse {
    val cached = Cache.get(topic)

    // Fast path: fresh cache and the caller did not ask to force a refresh.
    if (!force && cached != null && Cache.isFresh(topic, MAX_AGE_MS)) {
        return ContentResponse(topic, cached = true, updatedAt = cached.updatedAt, items = cached.items)
    }

    try {
        val items = refreshTopic(topic)
        if (items.isNotEmpty()) {
            return ContentResponse(topic, cached = false, updatedAt = System.currentTimeMillis(), items = items)
        }
    } catch (error: Exception) {
        System.err.println("[content] scrape failed for \"$topic\": ${error.message}")
    }

    // Last resort: give the user something old rather than an empty screen.
    if (cached != null && cached.items.isNotEmpty()) {
        return ContentResponse(topic, cached = true, stale = true, updatedAt = cached.updatedAt, items = cached.items)
    }

    return ContentResponse(topic, cached = false, updatedAt = null, items = emptyList())
}

// Merge every topic into one mixed list, taking turns (round-robin) so that
// the "For You" tab is a balanced blend instead of one topic at a time.
fun getMixedContent(): List<Item> {
    val lists = TOPICS.map { topic -> Cache.get(topic)?.items ?: emptyList() }
    val mixed = mutableListOf<Item>()
    val seen = mutableSetOf<String>()
    var index = 0

    while (true) {
        var addedSomething = false
        for (list in lists) {
            val item = list.getOrNull(index)
            if (item != null && seen.add(item.id)) {
                mixed.add(item)
                addedSomething = true
            }
        }
        if (!addedSomething) break
        index += 1
    }

    return mixed
}

// Same as getMixedContent, but if nothing has been cached yet (a very first
// visit, before the scheduler has run) we scrape every topic once so the
// "For You" tab is never empty.
suspend fun getMixedContentOrLoad(): List<Item> {
    if (getMixedContent().isNotEmpty()) return getMixedContent()

    println("[content] cold start: filling the cache for the first time...")
    coroutineScope {
        TOPICS.map { topic ->
            async(Dispatchers.IO) { getTopicContent(topic) }
        }.awaitAll()
    }
    return getMixedContent()
}
