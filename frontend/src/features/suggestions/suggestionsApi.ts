import { apiClient } from "../../lib/apiClient";
import type { Market } from "./market";

export interface MostActiveStock {
  ticker: string;
  name: string | null;
  price: number;
  changePercent: number;
  volume: number;
}

export const suggestionsApi = {
  mostActive: (limit: number, market: Market) =>
    apiClient.get<MostActiveStock[]>("/api/suggestions/most-active", { params: { limit, market } }),
};
