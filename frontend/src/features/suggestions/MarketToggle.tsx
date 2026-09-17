import type { Market } from "./market";
import "./marketToggle.css";

/** Switches which curated ticker universe "más activas", earnings, and
 * strategy scan — see StockUniverse.java on the backend. Favorites isn't
 * market-segmented (a favorite is just a ticker), so it doesn't take this. */
export function MarketToggle({ market, onChange }: { market: Market; onChange: (market: Market) => void }) {
  return (
    <div className="market-toggle">
      <span className="market-toggle-label">Mercado</span>
      <div className="market-toggle-buttons">
        <button type="button" className={market === "US" ? "active" : ""} onClick={() => onChange("US")}>
          🇺🇸 EE.UU.
        </button>
        <button
          type="button"
          className={market === "ARGENTINA" ? "active" : ""}
          onClick={() => onChange("ARGENTINA")}
        >
          🇦🇷 Argentina
        </button>
      </div>
    </div>
  );
}
