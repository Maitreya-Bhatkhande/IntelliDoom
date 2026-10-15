// labels.js
// ---------------------------------------------------------------------------
// Human-friendly names for topics and content types, plus one faint "glyph"
// per topic that is drawn on cards that have no image.
// Keeping them in one place makes the whole app easy to reword later.
// ---------------------------------------------------------------------------

export const TOPIC_LABELS = {
  all: "For You",
  essays: "Essays",
  mind: "Mind",
  science: "Science",
  maths: "Maths",
  tech: "Tech",
  news: "News",
  memes: "Memes",
};

// A single character per topic, shown very faintly as decoration on cards
// that have no image. Purely cosmetic - change them to anything you like.
export const TOPIC_GLYPHS = {
  all: "✳",
  essays: "¶",
  mind: "◉",
  science: "⚛",
  maths: "∑",
  tech: "⌘",
  news: "◇",
  memes: "☺",
};

export const TYPE_LABELS = {
  essay: "Essay",
  article: "Article",
  video: "Video",
  meme: "Meme",
  discussion: "Discussion",
};

// The tabs shown in the topic bar, in order.
export const TOPIC_TABS = [
  "all",
  "essays",
  "mind",
  "science",
  "maths",
  "tech",
  "news",
  "memes",
];
