import { type FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { authApi, startGoogleLogin } from "./authApi";
import { useGoogleLoginEnabled } from "./useGoogleLoginEnabled";
import "./auth.css";

export function RegisterPage() {
  const navigate = useNavigate();
  const googleLoginEnabled = useGoogleLoginEnabled();
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await authApi.register({ firstName, lastName, email, password });
      navigate("/verify-email", { state: { email } });
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        setError("Ya existe una cuenta con ese email.");
      } else if (axios.isAxiosError(err) && err.response?.data?.details?.length) {
        setError(err.response.data.details.join(" "));
      } else {
        setError("No se pudo completar el registro. Intentá de nuevo.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Crear cuenta</h1>
        {error && <div className="auth-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="auth-field">
            <label htmlFor="firstName">Nombre</label>
            <input id="firstName" required value={firstName} onChange={(e) => setFirstName(e.target.value)} />
          </div>
          <div className="auth-field">
            <label htmlFor="lastName">Apellido</label>
            <input id="lastName" required value={lastName} onChange={(e) => setLastName(e.target.value)} />
          </div>
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
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button className="auth-submit" type="submit" disabled={loading}>
            {loading ? "Creando cuenta..." : "Crear cuenta"}
          </button>
        </form>
        {googleLoginEnabled && (
          <>
            <div className="auth-divider">o</div>
            <button className="auth-google-button" type="button" onClick={startGoogleLogin}>
              Continuar con Google
            </button>
          </>
        )}
        <div className="auth-footer">
          ¿Ya tenés cuenta? <Link to="/login">Iniciá sesión</Link>
        </div>
      </div>
    </div>
  );
}
