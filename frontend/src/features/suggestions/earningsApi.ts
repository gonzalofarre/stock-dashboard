import { apiClient } from "../../lib/apiClient";
import type { Market } from "./market";

export interface UpcomingEarnings {
  ticker: string;
  companyName: string | null;
  reportDate: string; // ISO date (yyyy-MM-dd)
  time: "BEFORE_OPEN" | "AFTER_CLOSE" | "UNSPECIFIED";
}

export interface EarningsSurprise {
  ticker: string;
  companyName: string | null;
  reportDate: string; // ISO date (yyyy-MM-dd)
  surprisePercent: number;
}

export const earningsApi = {
  upcoming: (limit: number, market: Market) =>
    apiClient.get<UpcomingEarnings[]>("/api/suggestions/earnings/upcoming", { params: { limit, market } }),
  surprises: (limit: number, market: Market) =>
    apiClient.get<EarningsSurprise[]>("/api/suggestions/earnings/surprises", { params: { limit, market } }),
};
