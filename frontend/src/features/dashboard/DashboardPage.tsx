import { useAuth } from "../auth/AuthContext";

/** Placeholder — favorites and suggestion blocks land in Phase 2+. This just
 * proves the auth flow actually gets you somewhere protected. */
export function DashboardPage() {
  const { user, logout } = useAuth();

  return (
    <div style={{ padding: "2rem", fontFamily: "system-ui, sans-serif" }}>
      <h1>Hola, {user?.firstName} 👋</h1>
      <p>Sesión iniciada como {user?.email}.</p>
      <p style={{ color: "#666" }}>El dashboard real (favoritos + sugerencias) llega en la Fase 2.</p>
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
  );
}
