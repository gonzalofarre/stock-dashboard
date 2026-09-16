import { type FormEvent, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import axios from "axios";
import { authApi } from "./authApi";
import "./auth.css";

export function VerifyEmailPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const emailFromRegister = (location.state as { email?: string } | null)?.email ?? "";
  const [email, setEmail] = useState(emailFromRegister);
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setInfo(null);
    setLoading(true);
    try {
      await authApi.verifyEmail({ email, code });
      navigate("/login");
    } catch {
      setError("Código inválido o vencido.");
    } finally {
      setLoading(false);
    }
  }

  async function handleResend() {
    setError(null);
    setInfo(null);
    try {
      await authApi.resendVerification(email);
      setInfo("Te reenviamos el código.");
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        setError("No existe una cuenta con ese email.");
      } else {
        setError("No se pudo reenviar el código.");
      }
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Verificá tu email</h1>
        {error && <div className="auth-error">{error}</div>}
        {info && <div className="auth-success">{info}</div>}
        <form onSubmit={handleSubmit}>
          <div className="auth-field">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <div className="auth-field">
            <label htmlFor="code">Código de 6 dígitos</label>
            <input
              id="code"
              required
              maxLength={6}
              value={code}
              onChange={(e) => setCode(e.target.value)}
            />
          </div>
          <button className="auth-submit" type="submit" disabled={loading}>
            {loading ? "Verificando..." : "Verificar"}
          </button>
        </form>
        <div className="auth-footer">
          ¿No te llegó? <button type="button" onClick={handleResend} style={{ background: "none", border: "none", color: "#2563eb", fontWeight: 600, cursor: "pointer", padding: 0 }}>Reenviar código</button>
        </div>
      </div>
    </div>
  );
}
