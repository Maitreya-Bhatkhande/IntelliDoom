// client.js
// ---------------------------------------------------------------------------
// Every call to the backend goes through these tiny functions.
// In development Vite forwards "/api" to http://localhost:4000 (see
// vite.config.js), so a relative URL is all we need.
// ---------------------------------------------------------------------------

const API = "/api";

async function getJson(url) {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }
  return response.json();
}

// Ask the backend which topics exist.
export function fetchTopics() {
  return getJson(`${API}/topics`);
}

// Ask for a topic's content. Pass refresh:true to force a fresh scrape.
export function fetchContent(topic, { refresh = false } = {}) {
  const query = refresh ? "?refresh=1" : "";
  return getJson(`${API}/content/${topic}${query}`);
}

// Ask how old the cache is.
export function fetchStatus() {
  return getJson(`${API}/status`);
}

// Ask the backend for a picture when an item has none of its own.
// Returns an image URL, or null if nothing suitable was found.
export async function fetchImage(query, topic = "") {
  const params = `q=${encodeURIComponent(query)}&t=${encodeURIComponent(topic)}`;
  const data = await getJson(`${API}/image?${params}`);
  return data.image || null;
}
