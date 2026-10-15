// Card.jsx
// ---------------------------------------------------------------------------
// One card in the feed. Top = a media area, bottom = the text block.
//
// Media, in order of preference:
//   - a video   -> plays right here: while the card is on screen we swap the
//                  thumbnail for a YouTube player
//   - a picture -> shown whole (never stretched or cropped), with a blurred
//                  copy behind it to fill the empty space
//   - no picture-> we ask the backend for a related image (from Wikipedia)
//   - still none-> a faint topic glyph
// ---------------------------------------------------------------------------

import { useEffect, useRef, useState } from "react";
import { TOPIC_LABELS, TOPIC_GLYPHS, TYPE_LABELS } from "../labels.js";
import { fetchImage } from "../api/client.js";

// Pull the video id out of any kind of YouTube link.
function youTubeId(url) {
  try {
    const parsed = new URL(url);
    if (parsed.hostname === "youtu.be") return parsed.pathname.slice(1);
    if (parsed.hostname.endsWith("youtube.com")) {
      if (parsed.pathname === "/watch") return parsed.searchParams.get("v");
      const match = parsed.pathname.match(/^\/(?:embed|shorts|v)\/([^/]+)/);
      if (match) return match[1];
    }
  } catch (error) {
    // not a normal URL - just give up
  }
  return null;
}

// A friendly word per topic, used to find a related picture when the headline
// itself turns up nothing (so every card ends up with a picture).
const TOPIC_IMAGE_QUERY = {
  essays: "essay",
  mind: "philosophy",
  science: "science",
  maths: "mathematics",
  tech: "computer",
  news: "newspaper",
  memes: "humour",
};

export default function Card({ item, onOpen }) {
  const [active, setActive] = useState(false);
  const [fallback, setFallback] = useState(null);
  const [broken, setBroken] = useState(false);
  const cardRef = useRef(null);

  const videoId = item.type === "video" ? youTubeId(item.url) : null;
  const image = item.image || fallback;

  const topic = TOPIC_LABELS[item.topic] ?? item.topic;
  const type = TYPE_LABELS[item.type] ?? item.type;
  const glyph = TOPIC_GLYPHS[item.topic] ?? "◆";

  // Is this the card the user is looking at right now?
  useEffect(() => {
    const node = cardRef.current;
    if (!node) return undefined;
    const observer = new IntersectionObserver(
      ([entry]) => setActive(entry.isIntersecting),
      { threshold: 0.6 },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  // No picture of its own? Ask the backend for a related one: first from the
  // headline, then (if that finds nothing) from the topic.
  useEffect(() => {
    if (item.image) return undefined;
    let cancelled = false;

    async function find() {
      let url = await fetchImage(item.title).catch(() => null);
      if (!url) {
        const topicQuery = TOPIC_IMAGE_QUERY[item.topic] ?? item.topic;
        url = await fetchImage(topicQuery).catch(() => null);
      }
      if (!cancelled && url) setFallback(url);
    }

    find();
    return () => {
      cancelled = true;
    };
  }, [item.image, item.topic, item.title]);

  return (
    <article
      className="card"
      ref={cardRef}
      onClick={() => onOpen(item)}
      title={item.title}
    >
      {videoId && active ? (
        <div className="card-media">
          <div className="card-video" onClick={(event) => event.stopPropagation()}>
            <iframe
              src={`https://www.youtube.com/embed/${videoId}?autoplay=1&mute=1&rel=0&playsinline=1`}
              title={item.title}
              allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
              allowFullScreen
            />
          </div>
        </div>
      ) : image && !broken ? (
        <div className="card-media">
          <img className="card-glow" src={image} alt="" aria-hidden="true" />
          <img
            className="card-image"
            src={image}
            alt=""
            loading="lazy"
            onError={() => setBroken(true)}
          />
        </div>
      ) : (
        <div className="card-glyph">{glyph}</div>
      )}

      <div className="card-body">
        <div className="card-meta">
          <span className="card-tag">{type}</span>
          <span>{topic}</span>
          {item.score > 0 && <span className="card-score">▲ {item.score}</span>}
        </div>
        <h2 className="card-title">{item.title}</h2>
        {item.description && <p className="card-abstract">{item.description}</p>}
        <div className="card-foot">
          <span className="card-source">{item.source}</span>
          <span className="card-link">Open →</span>
        </div>
      </div>
    </article>
  );
}
