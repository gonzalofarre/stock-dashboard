import { useRef, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import "./tooltip.css";

/**
 * Wraps any inline content and shows an instant tooltip above it on hover,
 * via a React portal to document.body — TickerLink used to have its own
 * copy of exactly this. The portal is what makes it work reliably: some
 * places this is used live inside a table/list with overflow:hidden for
 * rounded corners, which would clip a normal absolutely-positioned tooltip.
 */
export function Tooltip({ text, children }: { text: string | null | undefined; children: ReactNode }) {
  const [hovering, setHovering] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const ref = useRef<HTMLSpanElement>(null);

  function handleEnter() {
    const rect = ref.current?.getBoundingClientRect();
    if (!rect) return;
    setPosition({ top: rect.top - 8, left: rect.left });
    setHovering(true);
  }

  if (!text) {
    return <>{children}</>;
  }

  return (
    <span ref={ref} onMouseEnter={handleEnter} onMouseLeave={() => setHovering(false)}>
      {children}
      {hovering &&
        createPortal(
          <span className="hover-tooltip" style={{ top: position.top, left: position.left }}>
            {text}
          </span>,
          document.body
        )}
    </span>
  );
}
