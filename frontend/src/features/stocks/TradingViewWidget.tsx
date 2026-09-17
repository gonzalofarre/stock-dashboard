import { useEffect, useRef } from "react";

/** Embeds TradingView's official "Advanced Chart" widget via their documented
 * script-embed pattern. The widget has no React-friendly update API, so a
 * symbol change tears down and re-mounts the whole thing rather than trying
 * to reconfigure it in place. */
export function TradingViewWidget({ symbol }: { symbol: string }) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    container.innerHTML = "";

    const widgetTarget = document.createElement("div");
    widgetTarget.className = "tradingview-widget-container__widget";
    container.appendChild(widgetTarget);

    const script = document.createElement("script");
    script.type = "text/javascript";
    script.src = "https://s3.tradingview.com/external-embedding/embed-widget-advanced-chart.js";
    script.async = true;
    script.innerHTML = JSON.stringify({
      autosize: true,
      symbol,
      interval: "15",
      timezone: "Etc/UTC",
      theme: "dark",
      style: "1",
      locale: "es",
      enable_publishing: false,
      allow_symbol_change: true,
      calendar: false,
      support_host: "https://www.tradingview.com",
    });
    container.appendChild(script);
  }, [symbol]);

  return <div className="tradingview-widget-container" ref={containerRef} />;
}
