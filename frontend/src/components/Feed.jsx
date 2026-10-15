// Feed.jsx
// The scrolling area. It shows one <Card> per item and asks for more whenever
// the user scrolls near the bottom — that is what makes the feed "endless".

import Card from "./Card.jsx";

export default function Feed({ items, loading, error, onLoadMore, onRetry, onOpen }) {
  function handleScroll(event) {
    const el = event.currentTarget;
    const nearBottom = el.scrollTop + el.clientHeight > el.scrollHeight - 900;
    if (nearBottom) onLoadMore();
  }

  // Nothing to show yet and something went wrong.
  if (error && items.length === 0) {
    return (
      <div className="feed-message">
        <p>{error}</p>
        <button className="retry" onClick={onRetry}>
          Try again
        </button>
      </div>
    );
  }

  // First ever load, before any items arrive.
  if (items.length === 0) {
    return (
      <div className="feed-message">
        <p>
          {loading
            ? "Gathering something worth thinking about…"
            : "Nothing here yet — the scraper may still be warming up."}
        </p>
        {!loading && (
          <button className="retry" onClick={onRetry}>
            Try again
          </button>
        )}
      </div>
    );
  }

  return (
    <main className="feed" onScroll={handleScroll}>
      {items.map((item) => (
        <Card key={item._key} item={item} onOpen={onOpen} />
      ))}
      {loading && <div className="feed-message small">…</div>}
    </main>
  );
}
