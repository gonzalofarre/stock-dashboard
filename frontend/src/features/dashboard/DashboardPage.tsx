import { useAuth } from "../auth/AuthContext";
import { FavoritesList } from "../favorites/FavoritesList";

/** The three suggestion blocks (Phases 3-5) and the TradingView chart detail
 * view (Phase 6) still aren't here — this is favorites only, per Phase 2. */
export function DashboardPage() {
  const { user, logout } = useAuth();

  return (
    <div style={{ padding: "2rem", fontFamily: "system-ui, sans-serif", maxWidth: "960px", margin: "0 auto" }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "2rem",
        }}
      >
        <div>
          <h1 style={{ margin: 0 }}>Hola, {user?.firstName} 👋</h1>
          <p style={{ color: "#666", margin: "0.25rem 0 0" }}>{user?.email}</p>
        </div>
        <button
          onClick={logout}
          style={{
            padding: "0.6rem 1.2rem",
            background: "#ef4444",
            color: "white",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer",
            fontWeight: 600,
          }}
        >
          Cerrar sesión
        </button>
      </div>

      <FavoritesList />

      <p style={{ color: "#999", fontSize: "0.85rem", marginTop: "2rem" }}>
        Las sugerencias automáticas (más activas, earnings, estrategia intradiaria) y el gráfico de TradingView
        llegan en las próximas fases.
      </p>
    </div>
  );
}
