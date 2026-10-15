// Routes.kt
// ---------------------------------------------------------------------------
// All of the API endpoints live here. The frontend only talks to these:
//
//   GET /api/topics                 -> ["all", "philosophy", ...]
//   GET /api/content/:topic         -> { topic, items: [...] }
//   GET /api/content/:topic?refresh=1  -> same, but forces a fresh scrape
//   GET /api/status                 -> when the cache was last updated
//   GET /api/image?q=...&t=...      -> a related image for one headline
//   GET /                           -> a friendly hello
//
// The JDK ships its own tiny HTTP server (com.sun.net.httpserver). We get one
// callback per request and simply pick a handler based on the URL path.
// ---------------------------------------------------------------------------

package com.intdoom

import com.sun.net.httpserver.HttpExchange
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString

// This runs for every request that reaches the server.
fun handle(exchange: HttpExchange) {
    try {
        // Let the Vite dev server (a different port) talk to this API.
        exchange.responseHeaders.set("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type")

        // The browser asks "may I?" before every cross-origin call.
        if (exchange.requestMethod == "OPTIONS") {
            exchange.sendResponseHeaders(204, -1)
            return
        }

        val path = exchange.requestURI.path // e.g. "/api/content/physics"
        val query = exchange.requestURI.query ?: "" // e.g. "refresh=1"

        when {
            // A friendly hello so you can check the server is alive in a browser.
            path == "/" -> sendJson(
                exchange, 200,
                json.encodeToString(
                    HelloResponse(
                        name = "IntelliDoom API",
                        message = "Curated rabbit holes for curious minds.",
                        endpoints = listOf("/api/topics", "/api/content/:topic", "/api/status", "/api/image"),
                    )
                )
            )

            path == "/api/topics" -> sendJson(exchange, 200, json.encodeToString(TopicsResponse(listTopics())))

            path == "/api/status" -> sendJson(exchange, 200, json.encodeToString(status()))

            // A related picture for an item that has none of its own.
            path == "/api/image" -> {
                val text = queryParam(query, "q")
                val topic = queryParam(query, "t")
                val image = if (text.isBlank()) null else runBlocking { findImage(text, topic) }
                sendJson(exchange, 200, json.encodeToString(ImageResponse(image)))
            }

            path.startsWith("/api/content/") -> {
                val topic = path.removePrefix("/api/content/").trim('/')

                if (topic !in listTopics()) {
                    sendJson(exchange, 404, json.encodeToString(ErrorResponse("Unknown topic \"$topic\".")))
                } else {
                    val response = runBlocking {
                        if (topic == "all") {
                            // "all" mixes the other topics together (scraping them if
                            // the cache is still empty, so the first visitor is not
                            // left staring at a blank feed).
                            ContentResponse(
                                topic = topic,
                                cached = true,
                                updatedAt = System.currentTimeMillis(),
                                items = getMixedContentOrLoad().shuffled(),
                            )
                        } else {
                            val result = getTopicContent(topic, force = query.contains("refresh=1"))
                            // Shuffle a copy so every visit shows a different order.
                            // This is what makes the endless feed feel endless.
                            result.copy(items = result.items.shuffled())
                        }
                    }
                    sendJson(exchange, 200, json.encodeToString(response))
                }
            }

            else -> sendJson(exchange, 404, json.encodeToString(ErrorResponse("Not found.")))
        }
    } catch (error: Exception) {
        System.err.println("[api] error: ${error.message}")
        try {
            sendJson(exchange, 500, json.encodeToString(ErrorResponse("Could not load content right now.")))
        } catch (alreadySent: Exception) {
            // The response was already on its way - nothing left to do.
        }
    } finally {
        exchange.close()
    }
}

// Read one value out of a query string like "q=hello%20world&t=tech".
private fun queryParam(query: String, name: String): String {
    for (part in query.split("&")) {
        val equals = part.indexOf('=')
        if (equals == -1) continue
        if (part.take(equals) == name) {
            return java.net.URLDecoder.decode(part.substring(equals + 1), "UTF-8")
        }
    }
    return ""
}

// Send an already-encoded JSON string as the response body.
private fun sendJson(exchange: HttpExchange, status: Int, text: String) {
    val bytes = text.toByteArray(Charsets.UTF_8)
    exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
    exchange.sendResponseHeaders(status, bytes.size.toLong())
    exchange.responseBody.use { it.write(bytes) }
}
