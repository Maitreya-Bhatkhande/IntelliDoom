// Scheduler.kt
// ---------------------------------------------------------------------------
// The scheduler is what makes the content "periodically refresh".
// It wakes up every CACHE_MINUTES, checks each topic, and re-scrapes any topic
// whose content has gone stale. It also does one warm-up run at startup so the
// first visitor does not have to wait for a cold scrape.
// ---------------------------------------------------------------------------

package com.intdoom

import kotlinx.coroutines.runBlocking

fun startScheduler() {
    Thread {
        // Warm-up shortly after boot (give the server a moment to start).
        Thread.sleep(2000)
        refreshStaleTopics()

        // Then keep refreshing forever, one pass per CACHE_MINUTES.
        while (true) {
            Thread.sleep(CACHE_MINUTES * 60 * 1000L)
            refreshStaleTopics()
        }
    }.apply {
        isDaemon = true // die together with the server, don't block shutdown
        start()
    }

    println("[scheduler] started. Refreshing every $CACHE_MINUTES minutes.")
}

private fun refreshStaleTopics() {
    for (topic in TOPICS) {
        if (Cache.isFresh(topic, MAX_AGE_MS)) continue
        try {
            runBlocking { refreshTopic(topic) }
        } catch (error: Exception) {
            System.err.println("[scheduler] could not refresh \"$topic\": ${error.message}")
        }
    }
}
