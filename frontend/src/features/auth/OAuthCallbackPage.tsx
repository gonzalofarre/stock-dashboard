import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import "./auth.css";

/** The backend redirects here after a successful Google login, with tokens
 * in the URL fragment (never sent to any server, only readable by this page's
 * own JS) — see OAuth2LoginSuccessHandler.java for why fragment over query string. */
export function OAuthCallbackPage() {
  const { completeOAuthLogin } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.slice(1));
    const accessToken = params.get("accessToken");
    const refreshToken = params.get("refreshToken");

    if (!accessToken || !refreshToken) {
      setError("No se recibieron los tokens de Google.");
      return;
    }

    completeOAuthLogin(accessToken, refreshToken)
      .then(() => navigate("/dashboard", { replace: true }))
      .catch(() => setError("No se pudo completar el login con Google."));
    // Intentionally runs once: this callback URL is only ever visited right
    // after the backend redirect, with a fresh fragment each time.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="auth-page">
      <div className="auth-card">
        {error ? <div className="auth-error">{error}</div> : <p>Completando inicio de sesión...</p>}
      </div>
    </div>
  );
}
