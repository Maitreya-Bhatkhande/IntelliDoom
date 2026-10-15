// TopicBar.jsx
// ---------------------------------------------------------------------------
// A quiet row of text tabs. Clicking one switches the feed's topic.
// No pill backgrounds: the active tab is simply brighter and underlined.
// ---------------------------------------------------------------------------

import { TOPIC_LABELS, TOPIC_TABS } from "../labels.js";

export default function TopicBar({ current, onChange }) {
  return (
    <nav className="topicbar">
      {TOPIC_TABS.map((id) => (
        <button
          key={id}
          className={id === current ? "tab active" : "tab"}
          onClick={() => onChange(id)}
        >
          {TOPIC_LABELS[id]}
        </button>
      ))}
    </nav>
  );
}
