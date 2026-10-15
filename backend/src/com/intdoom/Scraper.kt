// Scraper.kt
// ---------------------------------------------------------------------------
// The scraper downloads content from the internet and turns it into the simple
// Item shape defined in Types.kt.
//
// It contains four kinds of sources:
//   1. Reddit .rss         (best effort - often rate-limited, then skipped)
//   2. Hacker News API     (free, no key needed)
//   3. RSS / Atom feeds    (blogs, news sites, YouTube channels)
//   4. meme-api.com        (real memes from intellectual subreddits)
//   5. Wikipedia           (a related image when an item has none of its own)
//
// We only use tools that ship with the JDK (java.net.http) - no scraping
// libraries. Every download times out after 15 seconds so one slow site can
// never freeze the whole app.
// ---------------------------------------------------------------------------

package com.intdoom

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

// Some websites (like Reddit) block requests that look like robots.
// Giving a friendly name makes them treat us a little nicer.
private const val USER_AGENT = "IntelliDoom/1.0 (personal intellectual rabbit-hole demo)"

private const val MAX_REDDIT_POSTS = 25
private const val MAX_FEED_ITEMS = 12
private const val MAX_HN_STORIES = 20

// One HTTP client for the whole app (it keeps connections alive).
// Some feeds answer "301/308 moved" - always follow those, like browsers do.
private val http = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(15))
    .followRedirects(HttpClient.Redirect.ALWAYS)
    .build()

// ---------------------------------------------------------------------------
// Tiny download helpers.
// ---------------------------------------------------------------------------

fun getText(url: String): String {
    val request = HttpRequest.newBuilder(URI.create(url))
        .header("User-Agent", USER_AGENT)
        .timeout(Duration.ofSeconds(15))
        .build()
    val response = http.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) throw Exception("HTTP ${response.statusCode()} for $url")
    return response.body()
}

// Download a URL and parse it straight into a Kotlin class (see @Serializable).
inline fun <reified T> getJson(url: String): T = json.decodeFromString(getText(url))

// ---------------------------------------------------------------------------
// Small text helpers used by the feed parser.
// ---------------------------------------------------------------------------

fun decodeHtml(text: String): String =
    text
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace(Regex("""&#(\d+);""")) { match -> match.groupValues[1].toInt().toChar().toString() }

fun stripTags(text: String): String = decodeHtml(text.replace(Regex("<[^>]*>"), "").trim())

// Pull the text inside an XML tag, e.g. textOf(block, "title").
fun textOf(block: String, tag: String): String {
    val match = Regex("<$tag[^>]*>([\\s\\S]*?)</$tag>").find(block) ?: return ""
    // Feeds often wrap text in <![CDATA[ ... ]]>.
    val inner = match.groupValues[1].replace(Regex("""<!\[CDATA\[([\s\S]*?)]]>"""), "$1")
    return stripTags(inner)
}

// Atom feeds store links as <link href="..."/> instead of text inside <link>.
fun linkOf(block: String): String {
    val href = Regex("""<link[^>]*href="([^"]+)"""").find(block)
    if (href != null) return decodeHtml(href.groupValues[1])
    return textOf(block, "link")
}

// Try a few common ways an image can appear inside a feed item.
fun imageOf(block: String): String? {
    val patterns = listOf(
        Regex("""<media:thumbnail[^>]*url="([^"]+)""""),
        Regex("""<media:content[^>]*url="([^"]+)""""),
        Regex("""<enclosure[^>]*url="([^"]+\.(?:jpg|jpeg|png|gif|webp)[^"]*)""", RegexOption.IGNORE_CASE),
        Regex("""<img[^>]*src="([^"]+)""""),
    )
    for (pattern in patterns) {
        val match = pattern.find(block)
        if (match != null) return decodeHtml(match.groupValues[1])
    }
    return null
}

// Turn any date string from a feed into an ISO string, best effort.
// (RSS uses "Tue, 15 Oct 2026 10:00:00 GMT", YouTube uses ISO dates.)
fun parseDate(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return Instant.now().toString()
    return try {
        Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(trimmed)).toString()
    } catch (error: Exception) {
        try {
            Instant.parse(trimmed).toString()
        } catch (error2: Exception) {
            Instant.now().toString() // we don't know the date - assume "now"
        }
    }
}

// ---------------------------------------------------------------------------
// 1. Reddit (best effort).
//    We read the free ".rss" feed of a subreddit and reuse the feed parser.
//    Reddit often answers with "429 Too Many Requests"; when that happens the
//    request throws and scrapeTopic() just skips it. The app keeps working.
// ---------------------------------------------------------------------------

fun fetchReddit(source: RedditSource): List<Item> {
    val url = "https://www.reddit.com/r/${source.subreddit}/top/.rss?limit=$MAX_REDDIT_POSTS"
    return parseFeed(getText(url), topic = source.topic, source = "r/${source.subreddit}", type = "discussion")
}

// ---------------------------------------------------------------------------
// 2. Hacker News: a free, no-key API. First we get the top story ids,
//    then we download each story one by one (in parallel).
// ---------------------------------------------------------------------------

// The fields we care about. Everything is optional except id - the API adds
// and removes fields now and then, and ignoreUnknownKeys deals with that.
@Serializable
data class HnStory(
    val id: Int,
    val title: String? = null,
    val url: String? = null,
    val by: String? = null,
    val score: Int? = null,
    val text: String? = null,
    val time: Long? = null,
)

suspend fun fetchHackerNews(topic: String): List<Item> = coroutineScope {
    val ids = getJson<List<Int>>("https://hacker-news.firebaseio.com/v0/topstories.json")

    val stories = ids.take(MAX_HN_STORIES)
        .map { id ->
            async(Dispatchers.IO) {
                try {
                    getJson<HnStory>("https://hacker-news.firebaseio.com/v0/item/$id.json")
                } catch (error: Exception) {
                    null // one broken story must not break the list
                }
            }
        }
        .awaitAll()
        .filterNotNull()

    stories.map { story ->
        Item(
            id = "hn-${story.id}",
            topic = topic,
            type = "discussion",
            title = decodeHtml(story.title ?: ""),
            url = story.url ?: "https://news.ycombinator.com/item?id=${story.id}",
            image = null,
            source = "Hacker News",
            author = story.by,
            score = story.score ?: 0,
            description = story.text?.let { stripTags(it).take(220) } ?: "",
            publishedAt = Instant.ofEpochSecond(story.time ?: 0).toString(),
        )
    }
}

// ---------------------------------------------------------------------------
// 3. RSS / Atom feeds (blogs, news and YouTube).
//    We do not use a parser library: we just split the XML into <item>/<entry>
//    blocks and read the few tags we care about. Simple and dependency-free.
// ---------------------------------------------------------------------------

fun parseFeed(xml: String, topic: String, source: String, type: String): List<Item> {
    val blocks = Regex("""<(item|entry)[\s>][\s\S]*?</\1>""")
        .findAll(xml)
        .take(MAX_FEED_ITEMS)
        .toList()

    return blocks.mapIndexed { index, match ->
        val block = match.value
        val link = linkOf(block)
        val dateText = textOf(block, "published")
            .ifEmpty { textOf(block, "pubDate") }
            .ifEmpty { textOf(block, "updated") }

        Item(
            id = "feed-$source-$index-$link".replace(Regex("""\s+"""), "-").take(120),
            topic = topic,
            type = type,
            title = textOf(block, "title"),
            url = link,
            image = imageOf(block),
            source = source,
            author = textOf(block, "name").ifEmpty { null },
            score = 0,
            description = stripTags(
                textOf(block, "description")
                    .ifEmpty { textOf(block, "summary") }
                    .ifEmpty { textOf(block, "content") }
            ).take(220),
            publishedAt = parseDate(dateText),
        )
    }
}

fun fetchFeed(source: FeedSource): List<Item> =
    parseFeed(getText(source.url), topic = source.topic, source = source.source, type = source.type)

// ---------------------------------------------------------------------------
// 4. Memes: meme-api.com is a small free service that returns the most popular
//    posts of a subreddit as JSON - including the image url and the upvote
//    count - and needs no key. We ask one subreddit at a time to stay well
//    under its rate limit, and one bad subreddit never stops the others.
// ---------------------------------------------------------------------------

private const val MEMES_PER_SUBREDDIT = 25

// Only the fields we use. ignoreUnknownKeys deals with everything else.
@Serializable
data class MemeApiMeme(
    val subreddit: String = "",
    val title: String = "",
    val url: String = "",
    val postLink: String = "",
    val author: String = "",
    val ups: Int = 0,
)

@Serializable
data class MemeApiResponse(val memes: List<MemeApiMeme> = emptyList())

suspend fun fetchMemes(): List<Item> {
    // Ask each subreddit separately, then mix the results together. Without
    // this, the 80-item cap in organize() would keep only the last few
    // subreddits, so the Memes tab would look like one source over and over.
    val perSubreddit = mutableListOf<List<Item>>()
    val now = Instant.now().toString()

    for (subreddit in MEME_SUBREDDITS) {
        try {
            val response = getJson<MemeApiResponse>("https://meme-api.com/gimme/$subreddit/$MEMES_PER_SUBREDDIT")

            val fresh = response.memes
                .filter { it.url.isNotBlank() }
                .mapIndexed { index, meme ->
                    Item(
                        id = "meme-$subreddit-$index-${meme.postLink}".replace(Regex("""\s+"""), "-").take(120),
                        topic = MEME_TOPIC,
                        type = "meme",
                        title = decodeHtml(meme.title.ifBlank { "r/$subreddit" }),
                        url = meme.postLink.ifBlank { meme.url },
                        image = meme.url,
                        source = "r/${meme.subreddit.ifBlank { subreddit }}",
                        author = meme.author.ifBlank { null },
                        score = meme.ups,
                        description = "",
                        // Same time for every meme, so the mix order below is kept.
                        publishedAt = now,
                    )
                }

            perSubreddit.add(fresh)
        } catch (error: Exception) {
            System.err.println("[scraper] a source failed: ${error.message}")
        }
    }

    // Round-robin: one meme from each subreddit, then the next from each, ...
    val mixed = mutableListOf<Item>()
    var round = 0
    while (true) {
        var addedSomething = false
        for (list in perSubreddit) {
            val item = list.getOrNull(round)
            if (item != null) {
                mixed.add(item)
                addedSomething = true
            }
        }
        if (!addedSomething) break
        round += 1
    }

    return mixed
}

// ---------------------------------------------------------------------------
// 5. Related images (only used when an item has no picture of its own).
//    We ask Wikipedia for the article that best matches the headline, then
//    Wikimedia Commons for an image file, and finally fall back to a general
//    picture for the topic - so a card almost always has something to show.
// ---------------------------------------------------------------------------

// Just the bits of Wikipedia's reply that we need.
@Serializable
data class WikiThumbnail(val source: String = "")

@Serializable
data class WikiPage(val thumbnail: WikiThumbnail? = null)

@Serializable
data class WikiQuery(val pages: Map<String, WikiPage> = emptyMap())

@Serializable
data class WikiSearchResponse(val query: WikiQuery? = null)

// Wikimedia Commons returns image *files*, so there is always a url to use.
@Serializable
data class CommonsImageInfo(val thumburl: String = "", val url: String = "")

@Serializable
data class CommonsPage(val imageinfo: List<CommonsImageInfo> = emptyList())

@Serializable
data class CommonsQuery(val pages: Map<String, CommonsPage> = emptyMap())

@Serializable
data class CommonsSearchResponse(val query: CommonsQuery? = null)

// Remember answers for this run, so we look up each headline only once.
// An empty string means "we looked and found nothing".
private val imageCache = java.util.concurrent.ConcurrentHashMap<String, String>()

// Find a related image for a headline. Tries the headline first, then the
// topic. Returns null only if every attempt failed.
suspend fun findImage(query: String, topic: String = ""): String? {
    val key = "$topic|$query".trim().lowercase()
    if (key.isEmpty()) return null

    imageCache[key]?.let { return it.ifEmpty { null } }

    val cleaned = cleanQuery(query)
    val image = wikiThumbnail(cleaned)
        ?: commonsImage(cleaned)
        ?: wikiThumbnail(TOPIC_IMAGE[topic] ?: "")

    imageCache[key] = image ?: ""
    return image
}

// The picture on the best-matching Wikipedia article, if it has one.
private suspend fun wikiThumbnail(query: String): String? {
    if (query.isBlank()) return null
    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
    val url = "https://en.wikipedia.org/w/api.php?action=query&format=json" +
        "&generator=search&gsrsearch=$encoded&gsrlimit=5" +
        "&prop=pageimages&piprop=thumbnail&pithumbsize=1000"
    return try {
        getJson<WikiSearchResponse>(url).query?.pages?.values?.firstNotNullOfOrNull { it.thumbnail?.source }
    } catch (error: Exception) {
        null
    }
}

// An image *file* on Wikimedia Commons that matches the words in the headline.
private suspend fun commonsImage(query: String): String? {
    if (query.isBlank()) return null
    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
    val url = "https://commons.wikimedia.org/w/api.php?action=query&format=json" +
        "&generator=search&gsrsearch=$encoded&gsrnamespace=6&gsrlimit=3" +
        "&prop=imageinfo&iiprop=url&iiurlwidth=1000"
    return try {
        getJson<CommonsSearchResponse>(url).query?.pages?.values
            ?.firstNotNullOfOrNull { page -> page.imageinfo.firstOrNull()?.thumburl?.takeIf { it.isNotBlank() } }
    } catch (error: Exception) {
        null
    }
}

// Turn a messy headline into a cleaner search phrase.
private fun cleanQuery(title: String): String {
    return title
        .replace(Regex("(?i)^(show|ask)\\s+hn:?\\s*"), "")
        .replace(Regex("\\s+\\|\\s+.*$"), "")
        .replace(Regex("[\"\\[\\]“”‘’]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(120)
}

// A safe, general image per topic, used when nothing more specific is found.
private val TOPIC_IMAGE = mapOf(
    "essays" to "essay",
    "mind" to "philosophy",
    "science" to "science",
    "maths" to "mathematics",
    "tech" to "computer",
    "news" to "newspaper",
    "memes" to "humour",
)

// ---------------------------------------------------------------------------
// 6. The one function the rest of the app calls: scrape every source for a
//    topic at the same time, then clean up the results.
//
//    Each source runs on its own thread and failures are simply logged and
//    skipped - one dead website must never break the whole topic.
// ---------------------------------------------------------------------------

suspend fun scrapeTopic(topic: String): List<Item> = coroutineScope {
    // Build the list of "jobs": one small function per source.
    val jobs = mutableListOf<suspend () -> List<Item>>()

    if (REDDIT_ENABLED) {
        for (source in REDDIT_SOURCES) {
            if (source.topic == topic) jobs.add { fetchReddit(source) }
        }
    }
    for (source in FEED_SOURCES) {
        if (source.topic == topic) jobs.add { fetchFeed(source) }
    }
    if (topic == HACKER_NEWS_TOPIC) jobs.add { fetchHackerNews(topic) }
    if (topic == MEME_TOPIC) jobs.add { fetchMemes() }

    // Run every job at the same time and keep the ones that worked.
    val lists = jobs.map { job ->
        async(Dispatchers.IO) {
            try {
                job()
            } catch (error: Exception) {
                System.err.println("[scraper] a source failed: ${error.message}")
                emptyList()
            }
        }
    }.awaitAll()

    organize(lists.flatten())
}

// Remove junk + duplicates, then sort newest first (max 80 items).
private fun organize(items: List<Item>): List<Item> {
    val seen = mutableSetOf<String>()
    return items
        .filter { it.title.isNotBlank() && it.url.isNotBlank() && seen.add(it.id) }
        .sortedByDescending { item ->
            try {
                Instant.parse(item.publishedAt)
            } catch (error: Exception) {
                Instant.EPOCH
            }
        }
        .take(80)
}
