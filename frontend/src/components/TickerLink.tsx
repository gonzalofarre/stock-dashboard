import { useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Link } from "react-router-dom";
import "./tickerLink.css";

/**
 * A ticker that links to its detail page and shows the full company name on
 * hover. A native `title` attribute was tried first, but the browser's own
 * tooltip is slow to appear and easy to miss — this renders its own bubble
 * instantly via a React portal to `document.body`, so it isn't clipped by
 * any ancestor's `overflow: hidden` (several ticker lists live inside a
 * rounded, clipped table/card).
 */
export function TickerLink({ ticker, name, className }: { ticker: string; name?: string | null; className: string }) {
  const [hovering, setHovering] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const linkRef = useRef<HTMLAnchorElement>(null);

  function handleEnter() {
    const rect = linkRef.current?.getBoundingClientRect();
    if (!rect) return;
    setPosition({ top: rect.top - 8, left: rect.left });
    setHovering(true);
  }

  return (
    <>
      <Link
        ref={linkRef}
        to={`/stock/${ticker}`}
        className={className}
        onMouseEnter={handleEnter}
        onMouseLeave={() => setHovering(false)}
      >
        {ticker}
      </Link>
      {hovering &&
        name &&
        createPortal(
          <span className="ticker-tooltip" style={{ top: position.top, left: position.left }}>
            {name}
          </span>,
          document.body
        )}
    </>
  );
}
