import { apiClient } from "../../lib/apiClient";

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
  upcoming: (limit: number) =>
    apiClient.get<UpcomingEarnings[]>("/api/suggestions/earnings/upcoming", { params: { limit } }),
  surprises: (limit: number) =>
    apiClient.get<EarningsSurprise[]>("/api/suggestions/earnings/surprises", { params: { limit } }),
};
