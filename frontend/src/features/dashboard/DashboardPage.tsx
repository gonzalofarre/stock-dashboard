import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { FavoritesList } from "../favorites/FavoritesList";
import { MostActiveBlock } from "../suggestions/MostActiveBlock";
import { EarningsBlock } from "../suggestions/EarningsBlock";
import "./dashboard.css";

/** The intraday-strategy suggestion block (Phase 5) and the TradingView
 * chart detail view (Phase 6) still aren't here. */
export function DashboardPage() {
  const { user, logout } = useAuth();
  const [favoritesRefreshSignal, setFavoritesRefreshSignal] = useState(0);

  return (
    <div className="dashboard-page">
      <div className="dashboard-header">
        <div>
          <h1>Hola, {user?.firstName}</h1>
          <p>{user?.email}</p>
        </div>
        <button className="dashboard-logout" onClick={logout}>
          Cerrar sesión
        </button>
      </div>

      <FavoritesList refreshSignal={favoritesRefreshSignal} />
      <MostActiveBlock onFavoriteAdded={() => setFavoritesRefreshSignal((n) => n + 1)} />
      <EarningsBlock />

      <p className="dashboard-footnote">
        La sugerencia de estrategia intradiaria y el gráfico de TradingView llegan en las próximas fases.
      </p>
    </div>
  );
}
