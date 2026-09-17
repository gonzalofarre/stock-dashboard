import { apiClient } from "../../lib/apiClient";

export interface StrategySignal {
  ticker: string;
  type: "EMA_CROSSOVER" | "MACD_CROSSOVER";
  direction: "BULLISH" | "BEARISH";
  minutesAgo: number;
}

export const strategyApi = {
  signals: (limit: number) => apiClient.get<StrategySignal[]>("/api/suggestions/strategy", { params: { limit } }),
};
