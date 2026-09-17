import { useEffect, useState } from "react";
import { TickerLink } from "../../components/TickerLink";
import { earningsApi, type EarningsSurprise, type UpcomingEarnings } from "./earningsApi";
import "./suggestions.css";

function daysFromToday(dateStr: string): number {
  const target = new Date(`${dateStr}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86_400_000);
}

function formatUpcomingWhen(dateStr: string): string {
  const diff = daysFromToday(dateStr);
  if (diff <= 0) return "Hoy";
  if (diff === 1) return "Mañana";
  return `En ${diff} días`;
}

function formatPastWhen(dateStr: string): string {
  const diff = -daysFromToday(dateStr);
  if (diff <= 0) return "Hoy";
  if (diff === 1) return "Ayer";
  return `Hace ${diff} días`;
}

function formatTime(time: UpcomingEarnings["time"]): string {
  if (time === "BEFORE_OPEN") return "Antes de apertura";
  if (time === "AFTER_CLOSE") return "Después del cierre";
  return "";
}

/** Suggestion block 2: upcoming earnings + best recent earnings surprises,
 * both over the same curated liquid-stock universe as "most active" — see
 * StockUniverse.java. Both panels share one result-count control. */
export function EarningsBlock() {
  const [limit, setLimit] = useState(6);
  const [upcoming, setUpcoming] = useState<UpcomingEarnings[] | null>(null);
  const [surprises, setSurprises] = useState<EarningsSurprise[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setUpcoming(null);
    setSurprises(null);
    setError(null);
    Promise.all([earningsApi.upcoming(limit), earningsApi.surprises(limit)])
      .then(([upcomingRes, surprisesRes]) => {
        setUpcoming(upcomingRes.data);
        setSurprises(surprisesRes.data);
      })
      .catch(() => setError("No se pudo cargar la información de earnings."));
  }, [limit]);

  return (
    <div className="suggestion-block">
      <div className="suggestion-header">
        <h2>Próximos earnings</h2>
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

      <div className="earnings-grid">
        <div className="earnings-panel">
          <h3>Calendario</h3>
          {upcoming === null && !error && <p className="earnings-loading">Cargando...</p>}
          {upcoming?.length === 0 && <p className="earnings-empty">No hay earnings próximos en el radar.</p>}
          {upcoming && upcoming.length > 0 && (
            <ul className="earnings-list">
              {upcoming.map((e) => (
                <li key={e.ticker} className="earnings-row">
                  <TickerLink ticker={e.ticker} name={e.companyName} className="earnings-ticker" />
                  <span className="earnings-badge warn">
                    {formatUpcomingWhen(e.reportDate)}
                    {formatTime(e.time) ? ` · ${formatTime(e.time)}` : ""}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="earnings-panel">
          <h3>Mejores sorpresas recientes</h3>
          {surprises === null && !error && <p className="earnings-loading">Cargando...</p>}
          {surprises?.length === 0 && <p className="earnings-empty">No hay sorpresas recientes en el radar.</p>}
          {surprises && surprises.length > 0 && (
            <ul className="earnings-list">
              {surprises.map((s) => (
                <li key={s.ticker} className="earnings-row">
                  <TickerLink ticker={s.ticker} name={s.companyName} className="earnings-ticker" />
                  <span className={`earnings-surprise ${s.surprisePercent >= 0 ? "positive" : "negative"}`}>
                    {s.surprisePercent >= 0 ? "+" : ""}
                    {s.surprisePercent.toFixed(2)}%
                  </span>
                  <span className="earnings-when">{formatPastWhen(s.reportDate)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
