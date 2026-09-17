import { apiClient } from "../../lib/apiClient";

export interface StrategySignal {
  ticker: string;
  companyName: string | null;
  type: "EMA_CROSSOVER" | "MACD_CROSSOVER";
  direction: "BULLISH" | "BEARISH";
  minutesAgo: number;
  targetPrice: number;
}

export const strategyApi = {
  signals: (limit: number) => apiClient.get<StrategySignal[]>("/api/suggestions/strategy", { params: { limit } }),
};
