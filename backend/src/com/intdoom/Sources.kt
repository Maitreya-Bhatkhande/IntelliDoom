// Sources.kt
// ---------------------------------------------------------------------------
// This is the "map" of the app. It lists every place we get content from.
//
// The whole backend is just three ideas:
//   1. SOURCES  -> where content lives (this file)
//   2. SCRAPER  -> small functions that download + clean one source
//   3. CACHE    -> remembers the result so we don't download too often
//
// The categories are about *kinds of thinking*, not news sections:
//   essays  - long-form writing that makes you think (Aeon, Nautilus, ...)
//   mind    - philosophy + psychology
//   science - physics + chemistry, explained deeply
//   maths   - maths, logic and puzzles
//   tech    - code, computers and hacker culture
//   news    - current science/tech news (just one category)
//   memes   - real memes from intellectual subreddits (see MEME_SUBREDDITS)
//
// You can add a new topic or a new website by editing ONLY this file
// (plus a label + glyph in frontend/src/labels.js if you add a new topic).
// ---------------------------------------------------------------------------

package com.intdoom

// The topics shown as tabs in the app, in order.
// "all" is not listed here because it is built by mixing the others together.
val TOPICS = listOf(
    "essays",
    "mind",
    "science",
    "maths",
    "tech",
    "news",
    "memes",
)

// The topic that collects memes.
const val MEME_TOPIC = "memes"

// How long a topic's cached content stays "fresh" (in minutes).
// After this many minutes the scheduler (and the user) can ask for new content.
const val CACHE_MINUTES = 30

// Content types: "essay", "article", "video", "meme" or "discussion".
// (See the labels in frontend/src/labels.js.)

// ---------------------------------------------------------------------------
// 1. Reddit (best effort)
// ---------------------------------------------------------------------------
// Reddit is a good source of discussions, but it rate-limits servers very
// aggressively. We read its free ".rss" feed. If Reddit answers
// "429 Too Many Requests" the scraper simply skips it and the app still works.
data class RedditSource(
    val topic: String,
    val subreddit: String,
)

val REDDIT_SOURCES = listOf(
    // Mind
    RedditSource("mind", "philosophy"),
    RedditSource("mind", "psychology"),

    // Science
    RedditSource("science", "physics"),
    RedditSource("science", "chemistry"),

    // Maths
    RedditSource("maths", "math"),

    // Tech
    RedditSource("tech", "programming"),

    // News
    RedditSource("news", "science"),
)

// ---------------------------------------------------------------------------
// 2. RSS / Atom feeds (very reliable)
// ---------------------------------------------------------------------------
// Blogs, magazines, journals and YouTube channels all publish a free feed.
// type is "essay", "article" or "video".
data class FeedSource(
    val topic: String,
    val source: String,
    val url: String,
    val type: String = "essay",
)

val FEED_SOURCES = listOf(
    // ------------------------------ Essays --------------------------------
    FeedSource("essays", "Aeon", "https://aeon.co/feed.rss"),
    FeedSource("essays", "Nautilus", "https://nautil.us/feed/"),
    FeedSource("essays", "Psyche", "https://psyche.co/feed"),
    FeedSource("essays", "The Marginalian", "https://www.themarginalian.org/feed/"),
    FeedSource("essays", "3 Quarks Daily", "https://3quarksdaily.com/feed"),
    FeedSource("essays", "Noema", "https://www.noemamag.com/feed/"),
    FeedSource("essays", "Longreads", "https://longreads.com/feed/"),
    FeedSource("essays", "Asterisk Magazine", "https://asteriskmag.com/feed/"),
    FeedSource("essays", "Marginal Revolution", "https://marginalrevolution.com/feed"),
    FeedSource("essays", "Vsauce", "https://www.youtube.com/feeds/videos.xml?channel_id=UC6nSFpj9HTCZ5t-N3Rm3-HA", type = "video"),

    // ------------------------------- Mind ---------------------------------
    FeedSource("mind", "Daily Nous", "https://dailynous.com/feed/"),
    FeedSource("mind", "PsyPost", "https://www.psypost.org/feed/", type = "article"),
    FeedSource("mind", "Mind Hacks", "https://mindhacks.com/feed/"),
    FeedSource("mind", "Big Think", "https://www.youtube.com/feeds/videos.xml?channel_id=UCvQECJukTDE2i6aCoMnS-Vg", type = "video"),
    FeedSource("mind", "SciShow Psych", "https://www.youtube.com/feeds/videos.xml?channel_id=UCUdettijNYvLAm4AixZv4RA", type = "video"),
    FeedSource("mind", "Psych2Go", "https://www.youtube.com/feeds/videos.xml?channel_id=UCkJEpR7JmS36tajD34Gp4VA", type = "video"),

    // ----------------------------- Science --------------------------------
    FeedSource("science", "Quanta Magazine", "https://www.quantamagazine.org/feed/"),
    FeedSource("science", "Preposterous Universe", "https://www.preposterousuniverse.com/feed/"),
    FeedSource("science", "Backreaction", "https://backreaction.blogspot.com/feeds/posts/default"),
    FeedSource("science", "Not Even Wrong", "https://www.math.columbia.edu/~woit/wordpress/?feed=rss2"),
    FeedSource("science", "Veritasium", "https://www.youtube.com/feeds/videos.xml?channel_id=UCHnyfMqiRRG1u-2MsSQLbXA", type = "video"),
    FeedSource("science", "PBS Space Time", "https://www.youtube.com/feeds/videos.xml?channel_id=UC7_gcs09iThXybpVgjHZ_7g", type = "video"),
    FeedSource("science", "MinutePhysics", "https://www.youtube.com/feeds/videos.xml?channel_id=UCUHW94eEFW7hkUMVaZz4eDg", type = "video"),
    FeedSource("science", "Steve Mould", "https://www.youtube.com/feeds/videos.xml?channel_id=UCEIwxahdLz7bap-VDs9h35A", type = "video"),
    FeedSource("science", "Periodic Videos", "https://www.youtube.com/feeds/videos.xml?channel_id=UCtESv1e7ntJaLJYKIO1FoYw", type = "video"),
    FeedSource("science", "NileRed", "https://www.youtube.com/feeds/videos.xml?channel_id=UCFhXFikryT4aFcLkLw2LBLA", type = "video"),
    FeedSource("science", "Kurzgesagt", "https://www.youtube.com/feeds/videos.xml?channel_id=UCsXVk37bltHxD1rDPwtNM8Q", type = "video"),
    FeedSource("science", "The Royal Institution", "https://www.youtube.com/feeds/videos.xml?channel_id=UCYeF244yNGuFefuFKqxIAXw", type = "video"),
    FeedSource("science", "Sabine Hossenfelder", "https://www.youtube.com/feeds/videos.xml?channel_id=UC1yNl2E66ZzKApQdRuTQ4tw", type = "video"),

    // ------------------------------ Maths ---------------------------------
    FeedSource("maths", "The Aperiodical", "https://aperiodical.com/feed/"),
    FeedSource("maths", "Math with Bad Drawings", "https://mathwithbaddrawings.com/feed/"),
    FeedSource("maths", "Terence Tao", "https://terrytao.wordpress.com/feed/"),
    FeedSource("maths", "Gowers's Weblog", "https://gowers.wordpress.com/feed/"),
    FeedSource("maths", "3Blue1Brown", "https://www.youtube.com/feeds/videos.xml?channel_id=UCYO_jab_esuFRV4b17AJtAw", type = "video"),
    FeedSource("maths", "Numberphile", "https://www.youtube.com/feeds/videos.xml?channel_id=UCoxcjq-8xIDTYp3uz647V5A", type = "video"),
    FeedSource("maths", "Stand-up Maths", "https://www.youtube.com/feeds/videos.xml?channel_id=UCSju5G2aFaWMqn-_0YBtq5A", type = "video"),
    FeedSource("maths", "Up and Atom", "https://www.youtube.com/feeds/videos.xml?channel_id=UCSIvk78tK2TiviLQn4fSHaw", type = "video"),
    FeedSource("maths", "Tibees", "https://www.youtube.com/feeds/videos.xml?channel_id=UC52kszkc08-acFOuogFl5jw", type = "video"),
    FeedSource("maths", "Tom Rocks Maths", "https://www.youtube.com/feeds/videos.xml?channel_id=UCRfo-DAifrP3lzcxUHtGm_A", type = "video"),
    FeedSource("maths", "Welch Labs", "https://www.youtube.com/feeds/videos.xml?channel_id=UConVfxXodg78Tzh5nNu85Ew", type = "video"),

    // ------------------------------- Tech ---------------------------------
    FeedSource("tech", "Hackaday", "https://hackaday.com/feed/", type = "article"),
    FeedSource("tech", "Shtetl-Optimized", "https://scottaaronson.blog/?feed=rss2"),
    FeedSource("tech", "Hacker Noon", "https://hackernoon.com/feed", type = "article"),
    FeedSource("tech", "Computerphile", "https://www.youtube.com/feeds/videos.xml?channel_id=UC9-y-6csu5WGm29I7JiwpnA", type = "video"),
    FeedSource("tech", "Ben Eater", "https://www.youtube.com/feeds/videos.xml?channel_id=UCS0N5baNlQWJCUrhCEo8WlA", type = "video"),

    // ------------------------------- News ---------------------------------
    FeedSource("news", "MIT Technology Review", "https://www.technologyreview.com/feed/", type = "article"),
    FeedSource("news", "Ars Technica", "https://feeds.arstechnica.com/arstechnica/index", type = "article"),
    FeedSource("news", "Science News", "https://www.sciencenews.org/feed", type = "article"),
    FeedSource("news", "New Scientist", "https://www.newscientist.com/feed/home/", type = "article"),
    FeedSource("news", "Phys.org", "https://phys.org/rss-feed/physics-news/", type = "article"),
    FeedSource("news", "Phys.org Chemistry", "https://phys.org/rss-feed/chemistry-news/", type = "article"),
    FeedSource("news", "ScienceDaily Psychology", "https://www.sciencedaily.com/rss/mind_brain/psychology.xml", type = "article"),
)

// ---------------------------------------------------------------------------
// 3. Special JSON APIs (very reliable, no key needed)
// ---------------------------------------------------------------------------
// Hacker News needs a tiny bit of special handling, so it has its own label.
const val HACKER_NEWS_TOPIC = "tech"

// ---------------------------------------------------------------------------
// 4. Memes (real ones)
// ---------------------------------------------------------------------------
// Memes come from meme-api.com - a small free service that returns the top
// posts of a subreddit as JSON (title, image and upvotes) with no key needed.
// Reddit itself rate-limits servers hard, so we never ask Reddit for memes.
//
// Each subreddit below becomes its own "source" on the meme cards, e.g.
// "r/mathmemes", so the Memes tab shows a varied mix rather than one feed.
val MEME_SUBREDDITS = listOf(
    "mathmemes",
    "physicsmemes",
    "chemistrymemes",
    "biologymemes",
    "sciencememes",
    "engineeringmemes",
    "philosophymemes",
    "psychologymemes",
    "ProgrammerHumor",
)

// Reddit blocks many servers, so it is easy to switch off. Set to false if the
// server logs fill up with "429 Too Many Requests" - the app still works fine.
const val REDDIT_ENABLED = true
