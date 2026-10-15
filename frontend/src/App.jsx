// App.jsx
// ---------------------------------------------------------------------------
// The whole app is three pieces:
//   <TopicBar>  -> the row of topic buttons
//   <Feed>      -> the endless, snap-scrolling list
//   <Card>      -> one story / video / meme
//
// This component holds the shared state: which topic is selected, the list of
// items and whether we are loading.
// ---------------------------------------------------------------------------

import { useCallback, useEffect, useRef, useState } from "react";
import TopicBar from "./components/TopicBar.jsx";
import Feed from "./components/Feed.jsx";
import { fetchContent } from "./api/client.js";

export default function App() {
  const [topic, setTopic] = useState("all");
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // These two refs help us avoid messy race conditions without any libraries.
  const busy = useRef(false); // is a request already running?
  const requestId = useRef(0); // which request is the newest one?

  const loadFeed = useCallback(
    async ({ reset = false, refresh = false } = {}) => {
      // If we are already loading, only a "reset" (topic change / retry) wins.
      if (busy.current && !reset) return;

      const id = requestId.current + 1;
      requestId.current = id;
      busy.current = true;
      setLoading(true);

      try {
        const data = await fetchContent(topic, { refresh });

        // If the user switched topic while we were waiting, throw this away.
        if (id !== requestId.current) return;

        // Give every item a unique key so React is happy when the same
        // shuffled list is appended more than once (endless scrolling).
        const fresh = data.items.map((item, index) => ({
          ...item,
          _key: `${item.id}-${Date.now()}-${index}`,
        }));

        // When we append a new page, skip anything that already appeared in the
        // last few cards. The backend shuffles every request, so without this
        // the same story could show up twice in a row.
        setItems((previous) => {
          if (reset) return fresh;
          const recentIds = new Set(previous.slice(-4).map((entry) => entry.id));
          const filtered = fresh.filter((entry) => !recentIds.has(entry.id));
          return [...previous, ...filtered];
        });
        setError("");
      } catch {
        setError("Could not reach the backend. Is it running on port 4000?");
      } finally {
        if (id === requestId.current) {
          busy.current = false;
          setLoading(false);
        }
      }
    },
    [topic],
  );

  // When the topic changes, clear the feed and load the first page.
  useEffect(() => {
    setItems([]);
    loadFeed({ reset: true });
  }, [topic, loadFeed]);

  // Open a story in a new tab (the whole card is clickable).
  function openItem(item) {
    window.open(item.url, "_blank", "noopener,noreferrer");
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="brand-dot" />
          <h1>IntelliDoom</h1>
          <span className="brand-tag">curated rabbit holes for curious minds</span>
        </div>
        <button
          className="refresh"
          onClick={() => loadFeed({ reset: true, refresh: true })}
          disabled={loading}
          title="Fetch fresh content"
        >
          {loading ? "…" : "↻"}
        </button>
      </header>

      <TopicBar current={topic} onChange={setTopic} />

      <Feed
        items={items}
        loading={loading}
        error={error}
        onLoadMore={loadFeed}
        onRetry={() => loadFeed({ reset: true })}
        onOpen={openItem}
      />
    </div>
  );
}
