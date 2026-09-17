import { apiClient } from "../../lib/apiClient";

export interface Favorite {
  ticker: string;
  name: string | null;
  price: number | null;
  changePercent: number | null;
  quoteAvailable: boolean;
  addedAt: string;
}

export const favoritesApi = {
  list: () => apiClient.get<Favorite[]>("/api/favorites"),
  add: (ticker: string) => apiClient.post<void>("/api/favorites", { ticker }),
  remove: (ticker: string) => apiClient.delete<void>(`/api/favorites/${encodeURIComponent(ticker)}`),
};
