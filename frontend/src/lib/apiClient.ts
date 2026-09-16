import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import { authStorage } from "./authStorage";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

apiClient.interceptors.request.use((config) => {
  const token = authStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Concurrent requests that all 401 at once should trigger exactly ONE
// refresh call, not one per request — every failed request awaits the same
// in-flight refresh promise instead of racing to refresh independently.
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const refreshToken = authStorage.getRefreshToken();
  if (!refreshToken) {
    throw new Error("No refresh token available");
  }
  const response = await axios.post(`${import.meta.env.VITE_API_URL}/api/auth/refresh`, {
    refreshToken,
  });
  authStorage.setTokens(response.data.accessToken, response.data.refreshToken);
  return response.data.accessToken;
}

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retried?: boolean;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined;
    const isAuthEndpoint = originalRequest?.url?.includes("/api/auth/");

    if (error.response?.status === 401 && originalRequest && !originalRequest._retried && !isAuthEndpoint) {
      originalRequest._retried = true;
      try {
        refreshPromise ??= refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
        const newAccessToken = await refreshPromise;
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      } catch {
        authStorage.clear();
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);
