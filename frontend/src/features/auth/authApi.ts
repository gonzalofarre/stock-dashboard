import { apiClient } from "../../lib/apiClient";

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: { id: number; firstName: string; lastName: string; email: string };
}

export const authApi = {
  register: (data: { firstName: string; lastName: string; email: string; password: string }) =>
    apiClient.post<void>("/api/auth/register", data),

  verifyEmail: (data: { email: string; code: string }) =>
    apiClient.post<void>("/api/auth/verify-email", data),

  resendVerification: (email: string) =>
    apiClient.post<void>(`/api/auth/resend-verification?email=${encodeURIComponent(email)}`),

  login: (data: { email: string; password: string }) =>
    apiClient.post<AuthResponse>("/api/auth/login", data),

  logout: (refreshToken: string) => apiClient.post<void>("/api/auth/logout", { refreshToken }),

  me: () => apiClient.get<AuthResponse["user"]>("/api/auth/me"),

  config: () => apiClient.get<{ googleLoginEnabled: boolean }>("/api/auth/config"),
};

/** The backend does its own OAuth2 dance server-side; this just navigates the
 * whole page there — it's not an API call this SPA can make with fetch/axios. */
export function startGoogleLogin() {
  window.location.href = `${import.meta.env.VITE_API_URL}/oauth2/authorization/google`;
}
