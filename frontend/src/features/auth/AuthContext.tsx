import { createContext, useContext, useState, type ReactNode } from "react";
import { authStorage } from "../../lib/authStorage";
import { authApi, type AuthResponse } from "./authApi";

interface AuthUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  setAuthResponse: (response: AuthResponse) => void;
  completeOAuthLogin: (accessToken: string, refreshToken: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const USER_KEY = "authUser";

function loadStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY);
  return raw ? (JSON.parse(raw) as AuthUser) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadStoredUser());

  function setAuthResponse(response: AuthResponse) {
    authStorage.setTokens(response.accessToken, response.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(response.user));
    setUser(response.user);
  }

  async function login(email: string, password: string) {
    const { data } = await authApi.login({ email, password });
    setAuthResponse(data);
  }

  /** Used by OAuthCallbackPage: the tokens arrive from the backend's redirect,
   * but not the user's name/email — fetch those once the tokens are stored. */
  async function completeOAuthLogin(accessToken: string, refreshToken: string) {
    authStorage.setTokens(accessToken, refreshToken);
    const { data } = await authApi.me();
    localStorage.setItem(USER_KEY, JSON.stringify(data));
    setUser(data);
  }

  async function logout() {
    const refreshToken = authStorage.getRefreshToken();
    if (refreshToken) {
      // Best-effort: still clear local state even if the server call fails
      // (e.g. the token was already expired) — logging out locally should
      // never get stuck on a network request.
      await authApi.logout(refreshToken).catch(() => undefined);
    }
    authStorage.clear();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }

  return (
    <AuthContext.Provider
      value={{ user, isAuthenticated: user !== null, login, logout, setAuthResponse, completeOAuthLogin }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
