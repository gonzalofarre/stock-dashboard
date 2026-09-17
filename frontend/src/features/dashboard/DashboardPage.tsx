import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { FavoritesList } from "../favorites/FavoritesList";
import { MostActiveBlock } from "../suggestions/MostActiveBlock";
import { EarningsBlock } from "../suggestions/EarningsBlock";
import { StrategyBlock } from "../suggestions/StrategyBlock";
import { MarketToggle } from "../suggestions/MarketToggle";
import type { Market } from "../suggestions/market";
import { Brand } from "../../components/Brand";
import "./dashboard.css";

export function DashboardPage() {
  const { user, logout } = useAuth();
  const [favoritesRefreshSignal, setFavoritesRefreshSignal] = useState(0);
  const [market, setMarket] = useState<Market>("US");

  return (
    <div className="dashboard-page">
      <div className="dashboard-header">
        <div className="dashboard-header-main">
          <Brand size="sm" />
          <div>
            <h1>Hola, {user?.firstName}</h1>
            <p>{user?.email}</p>
          </div>
        </div>
        <button className="dashboard-logout" onClick={logout}>
          Cerrar sesión
        </button>
      </div>

      <MarketToggle market={market} onChange={setMarket} />

      <div className="dashboard-grid">
        <div className="dashboard-column">
          <FavoritesList refreshSignal={favoritesRefreshSignal} />
          <MostActiveBlock market={market} onFavoriteAdded={() => setFavoritesRefreshSignal((n) => n + 1)} />
        </div>
        <div className="dashboard-column">
          <EarningsBlock market={market} />
          <StrategyBlock market={market} />
        </div>
      </div>
    </div>
  );
}
