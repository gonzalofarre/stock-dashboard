import { apiClient } from "../../lib/apiClient";

export interface MostActiveStock {
  ticker: string;
  name: string | null;
  price: number;
  changePercent: number;
  volume: number;
}

export const suggestionsApi = {
  mostActive: (limit: number) =>
    apiClient.get<MostActiveStock[]>("/api/suggestions/most-active", { params: { limit } }),
};
