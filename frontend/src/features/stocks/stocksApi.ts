import { apiClient } from "../../lib/apiClient";

export interface StockQuote {
  ticker: string;
  name: string | null;
  price: number | null;
  changePercent: number | null;
  volume: number | null;
  quoteAvailable: boolean;
}

export interface TickerName {
  ticker: string;
  name: string;
}

export const stocksApi = {
  quote: (ticker: string) => apiClient.get<StockQuote>(`/api/stocks/${ticker}`),
  universe: () => apiClient.get<TickerName[]>("/api/stocks/universe"),
};
