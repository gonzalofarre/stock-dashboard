import { useEffect, useState } from "react";
import axios from "axios";
import { type MostActiveStock, suggestionsApi } from "./suggestionsApi";
import { favoritesApi } from "../favorites/favoritesApi";
import "./suggestions.css";

function formatVolume(volume: number): string {
  if (volume >= 1_000_000) return `${(volume / 1_000_000).toFixed(1)}M`;
  if (volume >= 1_000) return `${(volume / 1_000).toFixed(0)}K`;
  return String(volume);
}

/** Suggestion block 1: today's most active (highest-volume) stocks from a
 * curated liquid-stock universe — see StockUniverse.java for why it's not a
 * full market scan. Clicking a ticker to open a TradingView chart is Phase 6,
 * not built yet — tickers here are just labels for now, not links. */
export function MostActiveBlock({ onFavoriteAdded }: { onFavoriteAdded?: () => void } = {}) {
  const [limit, setLimit] = useState(10);
  const [stocks, setStocks] = useState<MostActiveStock[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [addedTickers, setAddedTickers] = useState<Set<string>>(new Set());

  useEffect(() => {
    setStocks(null);
    setError(null);
    suggestionsApi
      .mostActive(limit)
      .then(({ data }) => setStocks(data))
      .catch(() => setError("No se pudo cargar el listado de más activas."));
  }, [limit]);

  // Seed "already a favorite" state from what's actually on the server, once
  // on mount — otherwise a ticker favorited in a PREVIOUS visit still shows
  // an active "+ Favorita" button here even though it's already favorited.
  useEffect(() => {
    favoritesApi
      .list()
      .then(({ data }) => setAddedTickers(new Set(data.map((f) => f.ticker))))
      .catch(() => undefined); // non-critical — worst case the button just doesn't pre-disable
  }, []);

  async function handleAddFavorite(ticker: string) {
    try {
      await favoritesApi.add(ticker);
      setAddedTickers((current) => new Set(current).add(ticker));
      onFavoriteAdded?.();
    } catch (err) {
      // Either it was already a favorite, or something else failed — either
      // way, marking it "added" in the UI is the right outcome for a 409;
      // for anything else this at least avoids a broken repeat-click loop.
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        setAddedTickers((current) => new Set(current).add(ticker));
      }
    }
  }

  return (
    <div className="suggestion-block">
      <div className="suggestion-header">
        <h2>Más activas del día</h2>
        <select
          className="suggestion-limit-select"
          value={limit}
          onChange={(e) => setLimit(Number(e.target.value))}
        >
          {[5, 6, 7, 8, 9, 10].map((n) => (
            <option key={n} value={n}>
              Top {n}
            </option>
          ))}
        </select>
      </div>

      {error && <div className="suggestion-error">{error}</div>}
      {stocks === null && !error && <p>Cargando...</p>}

      {stocks && stocks.length > 0 && (
        <table className="suggestion-table">
          <thead>
            <tr>
              <th>Ticker</th>
              <th className="numeric">Precio</th>
              <th className="numeric">Var. %</th>
              <th className="numeric">Volumen</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {stocks.map((stock) => (
              <tr key={stock.ticker}>
                <td className="suggestion-ticker">{stock.ticker}</td>
                <td className="numeric">${stock.price.toFixed(2)}</td>
                <td className={`numeric suggestion-change ${stock.changePercent >= 0 ? "positive" : "negative"}`}>
                  {stock.changePercent >= 0 ? "+" : ""}
                  {stock.changePercent.toFixed(2)}%
                </td>
                <td className="numeric">{formatVolume(stock.volume)}</td>
                <td>
                  {addedTickers.has(stock.ticker) ? (
                    <span className="suggestion-fav-button already">En favoritas</span>
                  ) : (
                    <button className="suggestion-fav-button" onClick={() => handleAddFavorite(stock.ticker)}>
                      + Favorita
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
