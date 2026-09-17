import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { FavoritesList } from "../favorites/FavoritesList";
import { MostActiveBlock } from "../suggestions/MostActiveBlock";
import { EarningsBlock } from "../suggestions/EarningsBlock";
import { StrategyBlock } from "../suggestions/StrategyBlock";
import "./dashboard.css";

/** The TradingView chart detail view (Phase 6) still isn't here. */
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
      <StrategyBlock />

      <p className="dashboard-footnote">El gráfico de TradingView al hacer click en una acción llega en la próxima fase.</p>
    </div>
  );
}
