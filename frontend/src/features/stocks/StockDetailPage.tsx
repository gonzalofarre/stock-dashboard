import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import axios from "axios";
import { stocksApi, type StockQuote } from "./stocksApi";
import { favoritesApi } from "../favorites/favoritesApi";
import { TradingViewWidget } from "./TradingViewWidget";
import "./stockDetail.css";

function StarIcon({ filled }: { filled: boolean }) {
  const path =
    "M12 2.5l3.09 6.26 6.91 1.01-5 4.87 1.18 6.87L12 17.98 5.82 21.51 7 14.64l-5-4.87 6.91-1.01L12 2.5z";
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" aria-hidden="true">
      <path d={path} fill={filled ? "currentColor" : "none"} stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
    </svg>
  );
}

/** Sugerencia detail view (Phase 6): quote header + official TradingView
 * chart for whichever ticker was clicked, from any suggestion list or
 * favorites. */
export function StockDetailPage() {
  const { ticker = "" } = useParams<{ ticker: string }>();
  const navigate = useNavigate();
  const symbol = ticker.toUpperCase();

  const [quote, setQuote] = useState<StockQuote | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isFavorite, setIsFavorite] = useState(false);
  const [favoriteBusy, setFavoriteBusy] = useState(false);

  useEffect(() => {
    setQuote(null);
    setError(null);
    stocksApi
      .quote(symbol)
      .then(({ data }) => setQuote(data))
      .catch(() => setError("No se pudo cargar la cotización."));

    favoritesApi
      .list()
      .then(({ data }) => setIsFavorite(data.some((f) => f.ticker === symbol)))
      .catch(() => undefined);
  }, [symbol]);

  async function toggleFavorite() {
    setFavoriteBusy(true);
    try {
      if (isFavorite) {
        await favoritesApi.remove(symbol);
        setIsFavorite(false);
      } else {
        await favoritesApi.add(symbol);
        setIsFavorite(true);
      }
    } catch (err) {
      // A 409 on add just means it was already there server-side — reflect that either way.
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        setIsFavorite(true);
      }
    } finally {
      setFavoriteBusy(false);
    }
  }

  return (
    <div className="stock-detail-page">
      <button className="stock-detail-back" onClick={() => navigate(-1)}>
        ← Volver
      </button>

      <div className="stock-detail-header">
        <div>
          <h1>{symbol}</h1>
          {quote?.name && <p className="stock-detail-company-name">{quote.name}</p>}
          {error && <div className="suggestion-error">{error}</div>}
          {!error && quote === null && <p className="stock-detail-loading">Cargando...</p>}
          {quote?.quoteAvailable && (
            <div className="stock-detail-price-row">
              <span className="stock-detail-price">${quote.price?.toFixed(2)}</span>
              <span className={`stock-detail-change ${(quote.changePercent ?? 0) >= 0 ? "positive" : "negative"}`}>
                {(quote.changePercent ?? 0) >= 0 ? "+" : ""}
                {quote.changePercent?.toFixed(2)}%
              </span>
            </div>
          )}
          {quote && !quote.quoteAvailable && <p className="stock-detail-unavailable">Sin datos de cotización</p>}
        </div>
        <button className="stock-detail-fav-button" onClick={toggleFavorite} disabled={favoriteBusy}>
          <StarIcon filled={isFavorite} />
          {isFavorite ? "En favoritas" : "Agregar a favoritas"}
        </button>
      </div>

      <div className="stock-detail-chart-card">
        <TradingViewWidget symbol={symbol} />
      </div>
    </div>
  );
}
