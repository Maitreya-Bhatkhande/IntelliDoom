// Types.kt
// ---------------------------------------------------------------------------
// The data shapes used all over the app. The @Serializable annotation lets
// kotlinx.serialization turn these classes into JSON (and back) with a single
// line, so we never have to build JSON strings by hand.
//
// Every content item in the feed looks like this:
//
//   Item(
//     id: "reddit-abc123",          // unique string
//     topic: "physics",             // one of TOPICS
//     type: "article",              // essay | article | video | meme | discussion
//     title: "Why time is weird",
//     url: "https://...",           // where the user should go
//     image: "https://..." | null,  // optional preview image
//     source: "Veritasium",         // who published it
//     author: "derek",              // optional
//     score: 1234,                  // upvotes / points (0 if unknown)
//     description: "short summary", // optional
//     publishedAt: "2026-01-01T00:00:00.000Z",
//   )
// ---------------------------------------------------------------------------

package com.intdoom

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// The JSON settings. ignoreUnknownKeys means "if a website adds extra fields
// we don't know about, just ignore them instead of crashing".
val json = Json { ignoreUnknownKeys = true }

// One item in the feed (see the comment above).
@Serializable
data class Item(
    val id: String,
    val topic: String,
    val type: String,
    val title: String,
    val url: String,
    val image: String? = null,
    val source: String,
    val author: String? = null,
    val score: Int = 0,
    val description: String = "",
    val publishedAt: String,
)

// ---------------------------------------------------------------------------
// The shapes of our own API responses (one per endpoint).
// ---------------------------------------------------------------------------

// GET /  ->  {"name": "IntelliDoom API", ...}
@Serializable
data class HelloResponse(
    val name: String,
    val message: String,
    val endpoints: List<String>,
)

// GET /api/topics  ->  {"topics": ["all", "philosophy", ...]}
@Serializable
data class TopicsResponse(val topics: List<String>)

// GET /api/content/:topic  ->  {"topic": ..., "cached": ..., "items": [...]}
// (stale is left out of the JSON unless it is true)
@Serializable
data class ContentResponse(
    val topic: String,
    val cached: Boolean,
    val updatedAt: Long? = null,
    val items: List<Item> = emptyList(),
    val stale: Boolean = false,
)

// One topic's summary inside the status reply.
@Serializable
data class TopicMeta(val count: Int, val updatedAt: Long)

// GET /api/status  ->  {"cacheMinutes": 30, "topics": {"physics": {...}}}
@Serializable
data class StatusResponse(
    val cacheMinutes: Int,
    val topics: Map<String, TopicMeta>,
)

// GET /api/image?q=...  ->  {"image": "https://..."}  (image is null if none)
@Serializable
data class ImageResponse(val image: String? = null)

// Any error  ->  {"error": "some message"}
@Serializable
data class ErrorResponse(val error: String)
