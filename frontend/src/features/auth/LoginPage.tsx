import { type FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { useAuth } from "./AuthContext";
import { startGoogleLogin } from "./authApi";
import { useGoogleLoginEnabled } from "./useGoogleLoginEnabled";
import { GoogleIcon } from "./GoogleIcon";
import "./auth.css";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const googleLoginEnabled = useGoogleLoginEnabled();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
      navigate("/dashboard");
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        const message = err.response.data?.message;
        setError(message === "Email not verified" ? "Verificá tu email antes de iniciar sesión." : "Email o contraseña incorrectos.");
      } else {
        setError("No se pudo iniciar sesión. Intentá de nuevo.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Iniciar sesión</h1>
        {error && <div className="auth-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="auth-field">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <div className="auth-field">
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button className="auth-submit" type="submit" disabled={loading}>
            {loading ? "Ingresando..." : "Ingresar"}
          </button>
        </form>
        {googleLoginEnabled && (
          <>
            <div className="auth-divider">o</div>
            <button className="auth-google-button" type="button" onClick={startGoogleLogin}>
              <GoogleIcon />
              Continuar con Google
            </button>
          </>
        )}
        <div className="auth-footer">
          ¿No tenés cuenta? <Link to="/register">Registrate</Link>
        </div>
      </div>
    </div>
  );
}
