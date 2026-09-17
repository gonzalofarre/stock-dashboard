import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { strategyApi, type StrategySignal } from "./strategyApi";
import "./suggestions.css";

const TYPE_LABEL: Record<StrategySignal["type"], string> = {
  EMA_CROSSOVER: "Cruce EMA 9/21",
  MACD_CROSSOVER: "Cruce MACD",
};

function formatMinutesAgo(minutes: number): string {
  if (minutes < 60) return `Hace ${minutes} min`;
  const hours = Math.round(minutes / 60);
  return `Hace ${hours} h`;
}

function DirectionIcon({ direction }: { direction: StrategySignal["direction"] }) {
  const points = direction === "BULLISH" ? "6 18 12 6 18 18" : "6 6 12 18 18 6";
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <polyline
        points={points}
        stroke="currentColor"
        strokeWidth="2.4"
        strokeLinecap="round"
        strokeLinejoin="round"
        fill="none"
      />
    </svg>
  );
}

/** Suggestion block 3: intraday EMA9/21 and MACD crossovers over the same
 * curated universe as the other suggestion blocks — see StrategyService.java.
 * Extensible for more indicators later (RSI, Bollinger): a new SignalType
 * only needs a label here, nothing structural changes. Unlike the other two
 * blocks, this can legitimately come back with fewer than `limit` results —
 * a scan just might not find that many fresh crossovers right now. */
export function StrategyBlock() {
  const [limit, setLimit] = useState(10);
  const [signals, setSignals] = useState<StrategySignal[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setSignals(null);
    setError(null);
    strategyApi
      .signals(limit)
      .then(({ data }) => setSignals(data))
      .catch(() => setError("No se pudo cargar la estrategia intradiaria."));
  }, [limit]);

  return (
    <div className="suggestion-block">
      <div className="suggestion-header">
        <h2>Señal técnica intradía</h2>
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
      {signals === null && !error && <p className="earnings-loading">Cargando...</p>}
      {signals?.length === 0 && (
        <p className="earnings-empty">No hay cruces de EMA o MACD frescos en este momento — probá de nuevo en un rato.</p>
      )}

      {signals && signals.length > 0 && (
        <div className="signal-list">
          {signals.map((s, i) => (
            <div key={`${s.ticker}-${s.type}-${i}`} className="signal-row">
              <span className={`signal-icon ${s.direction === "BULLISH" ? "bullish" : "bearish"}`}>
                <DirectionIcon direction={s.direction} />
              </span>
              <Link to={`/stock/${s.ticker}`} className="signal-ticker" title={s.companyName ?? undefined}>
                {s.ticker}
              </Link>
              <span className="signal-label">{TYPE_LABEL[s.type]}</span>
              <span className="signal-target">
                Target <strong>${s.targetPrice.toFixed(2)}</strong>
              </span>
              <span className={`signal-badge ${s.direction === "BULLISH" ? "bullish" : "bearish"}`}>
                {s.direction === "BULLISH" ? "Alcista" : "Bajista"}
              </span>
              <span className="signal-when">{formatMinutesAgo(s.minutesAgo)}</span>
            </div>
          ))}
        </div>
      )}

      {signals && signals.length > 0 && (
        <p className="signal-disclaimer">
          El target es una proyección técnica simple (precio actual + la distancia entre las dos líneas del cruce) —
          no es una recomendación de inversión.
        </p>
      )}
    </div>
  );
}
