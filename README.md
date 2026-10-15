# IntelliDoom ✳

**Curated rabbit holes for curious minds.** An endless, full-screen,
snap-scrolling feed of things worth thinking about — long-form essays, deep-dive
videos, maths and logic, code and computers, a little news, and intellectual
memes.

The backend **scrapes the web periodically and on demand, caches the results**,
and the frontend shows them in one card per screen.

Built with **React + Vite** (frontend) and **Kotlin** (backend) only.

---

## The categories

IntelliDoom is organised around *kinds of thinking*, not news sections. News is
just one category among many:

| Tab         | What's in it                                                             |
| ----------- | ------------------------------------------------------------------------ |
| **Essays**  | Long-form, idea-rich writing — Aeon, Nautilus, Psyche, The Marginalian…  |
| **Mind**    | Philosophy + psychology — Daily Nous, PsyPost, Big Think, SciShow Psych   |
| **Science** | Physics + chemistry, explained deeply — Quanta, Veritasium, PBS Space Time |
| **Maths**   | Maths, logic and puzzles — 3Blue1Brown, Numberphile, Terence Tao          |
| **Tech**    | Code, computers and hacker culture — Hacker News, Hackaday, Ben Eater     |
| **News**    | Current science/tech news (the *only* newsy tab)                         |
| **Memes**   | Real memes from intellectual subreddits — r/mathmemes, r/physicsmemes, r/PhilosophyMemes, r/ProgrammerHumor, … |

---

## Project structure

```
IntDoom/                         # the folder keeps its original name
├── package.json                 # root scripts (run both servers with one command)
├── dev.js                       # starts backend + frontend together
├── backend/                     # the Kotlin API
│   ├── build.bat                # compiles the Kotlin code  -> build\classes
│   ├── run.bat                  # runs the server on http://localhost:4000
│   ├── lib/                     # the Kotlin/JSON/coroutines jars (no install needed)
│   ├── tools/                   # bundled, portable toolchain (nothing to install)
│   │   ├── kotlinc/             #   the Kotlin compiler
│   │   └── jre/                 #   a Java runtime
│   └── src/com/intdoom/
│       ├── Main.kt              # starts the HTTP server + the scheduler
│       ├── Routes.kt            # the /api endpoints (URL -> handler)
│       ├── Sources.kt           # the "map": every website we scrape
│       ├── Scraper.kt           # downloads + cleans each source
│       ├── Cache.kt             # remembers content (memory + JSON file)
│       ├── ContentService.kt    # decides: use cache, or scrape fresh?
│       ├── Scheduler.kt         # re-scrapes every 30 minutes
│       └── Types.kt             # the data classes (Item, API responses)
│
├── frontend/                    # the React app (Vite)
│   ├── index.html
│   ├── vite.config.js           # forwards /api -> http://localhost:4000
│   └── src/
│       ├── main.jsx             # React entry point
│       ├── App.jsx              # shared state + layout
│       ├── labels.js            # topic / type names + faint card glyphs
│       ├── api/client.js        # tiny functions that call the backend
│       ├── components/
│       │   ├── TopicBar.jsx     # the row of text tabs
│       │   ├── Feed.jsx         # the endless scrolling list
│       │   └── Card.jsx         # one essay / video / meme
│       └── styles/index.css     # all the styling in one file
└── README.md
```

Every file is short and commented, so a beginner can read it top to bottom.

---

## How it works

```
Browser (React)
   │  GET /api/content/science      (Vite proxies /api to port 4000)
   ▼
Kotlin route (Routes.kt)
   │
   ▼
ContentService  ── fresh cache? ──► yes ──► return cached items (instant)
   │
   └─ no ─► Scraper ─► Reddit .rss + RSS/YouTube feeds + Hacker News + meme-api
                          │
                          └─► Cache (memory + backend/.cache/content.json)
```

- The **scraper** turns every source into the same simple shape (`Item` in
  `Types.kt`: id, topic, type, title, url, image, source, score, …).
- The **cache** means we only download a topic once every 30 minutes.
- The **scheduler** refreshes stale topics in the background. A "cold" scrape
  also happens automatically the first time someone asks for a topic.
- One broken or rate-limited website never breaks a topic: the scraper waits
  for all sources and simply keeps the ones that worked.

### Content sources

| Kind            | Examples                                                                                                                            |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| RSS / Atom      | Aeon, Nautilus, Psyche, The Marginalian, 3 Quarks Daily, Noema, Longreads, Marginal Revolution, Quanta, Daily Nous, Mind Hacks, Terence Tao, Hackaday, Ars Technica, MIT Tech Review, Science News, New Scientist, Phys.org |
| YouTube feeds   | Veritasium, 3Blue1Brown, Kurzgesagt, Numberphile, Computerphile, Ben Eater, PBS Space Time, Sabine Hossenfelder, Steve Mould, Vsauce |
| Memes (JSON)    | meme-api.com — real memes from r/mathmemes, r/physicsmemes, r/chemistrymemes, r/biologymemes, r/sciencememes, r/engineeringmemes, r/PhilosophyMemes, r/psychologymemes, r/ProgrammerHumor |
| Hacker News API | top stories                                                                                                                          |
| Reddit `.rss`   | r/philosophy, r/math, r/programming, r/science (discussions only)                                                                   |

> Reddit aggressively rate-limits servers. If your server logs fill up with
> `429 Too Many Requests`, open `backend/src/com/intdoom/Sources.kt` and set
> `REDDIT_ENABLED = false`. Everything else keeps working.

---

## Running the app

You need only **Node.js 18 or newer** (for the frontend) — you already have it.

The backend needs **no installs at all**: the Kotlin compiler and a Java runtime
are bundled in `backend/tools/`, and `build.bat` / `run.bat` find them through
relative paths. No Gradle, no JDK, no Kotlin to install.

### Easiest way — one command

From the project root:

```bash
npm run build    # compiles the Kotlin backend (first time, and after Kotlin changes)
npm run dev      # starts BOTH servers
```

Then open <http://localhost:5173>. Press `Ctrl+C` to stop everything.

> `npm run dev` also compiles by itself if you never built before.

### Or run each part in its own terminal

**Terminal 1 — backend**

```bash
cd backend
run.bat          # http://localhost:4000  (builds first if needed)
```

**Terminal 2 — frontend**

```bash
cd frontend
npm install      # once
npm run dev      # http://localhost:5173
```

> The first visit scrapes the topics, which takes a few seconds. After that the
> cache makes it instant.

### Editing Kotlin code

1. Change a file in `backend/src/com/intdoom/`
2. `npm run build` (or `backend\build.bat`)
3. Restart the backend (`run.bat`) — the old process must be stopped first

---

## API endpoints

| Method | Endpoint                        | What it does                                  |
| ------ | ------------------------------- | --------------------------------------------- |
| GET    | `/api/topics`                   | list of topics (`["all", "essays", "mind", …]`) |
| GET    | `/api/content/:topic`           | content for a topic (scrapes if stale)        |
| GET    | `/api/content/:topic?refresh=1` | force a fresh scrape                          |
| GET    | `/api/status`                   | when each topic's cache was last updated      |
| GET    | `/api/image?q=...&t=...`        | a related image for a headline that has none  |

Try it: <http://localhost:4000/api/content/memes>

---

## Adding your own content

Everything is driven by **one file**: `backend/src/com/intdoom/Sources.kt`.

- **New website (RSS/Atom feed):** add one line to `FEED_SOURCES`:
  ```kotlin
  FeedSource("maths", "My Blog", "https://example.com/feed"),
  ```
  The fourth argument is the type: `"essay"`, `"article"` or `"video"`
  (the default is `"essay"`).
- **New subreddit (discussions):** add one line to `REDDIT_SOURCES`:
  ```kotlin
  RedditSource("maths", "myfavouritesub"),
  ```
- **New meme subreddit:** add its name to `MEME_SUBREDDITS` in
  `backend/src/com/intdoom/Sources.kt` (memes come from meme-api.com, which is
  far more reliable than Reddit itself):
  ```kotlin
  MEME_SUBREDDITS = listOf("mathmemes", "myfavouritememesub", ...)
  ```
- **New topic:** add its name to `TOPICS`, add sources for it, and add a label
  and a glyph to `frontend/src/labels.js`.

No other code needs to change.

---

## Tech stack

- **Frontend:** React 18 + Vite, plain CSS. No UI libraries.
- **Backend:** Kotlin + the JDK's built-in HTTP server + `java.net.http`
  (scraping) + kotlinx-coroutines (parallel downloads) + kotlinx-serialization
  (JSON). No web framework, no Gradle.
- **Build:** `backend/build.bat` uses the bundled `tools\kotlinc` and writes to
  `build\classes`; `run.bat` starts it with the bundled `tools\jre`.
