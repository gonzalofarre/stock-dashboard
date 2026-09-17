package com.stockdashboard.suggestions;

import java.util.List;
import java.util.Map;

/**
 * Hand-picked ticker universes that "most active", "strategy", and earnings
 * rank/filter over — one per {@link Market}. Deliberately NOT a full-market
 * scan: see the class-level reasoning that used to live here, still true —
 * Twelve Data's free tier doesn't give confident access to a dedicated
 * screener endpoint, so this reuses the already-tested per-symbol quote
 * infrastructure (see marketdata/QuoteCacheService) over a curated list
 * instead.
 *
 * {@code ARGENTINA_TICKERS} is Argentine companies' US-listed ADRs (priced
 * in USD on NYSE/NASDAQ), NOT their BYMA/Merval-listed local shares (priced
 * in ARS) — Twelve Data's free tier only covers ~3 markets, and BYMA isn't
 * one of them. Every ticker below was individually confirmed against a real
 * Twelve Data /quote response before being added; don't add one without
 * doing the same; a plausible-looking ticker for a delisted or ARS-only
 * line would fail silently (quoteAvailable: false) rather than error.
 */
public final class StockUniverse {

    public static final List<String> US_TICKERS = List.of(
            "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA", "AVGO", "JPM", "V",
            "UNH", "XOM", "JNJ", "WMT", "PG", "MA", "HD", "CVX", "MRK", "ABBV",
            "KO", "PEP", "BAC", "COST", "TMO", "MCD", "CSCO", "ACN", "ABT", "DHR",
            "LIN", "ADBE", "CRM", "NFLX", "AMD", "INTC", "QCOM", "TXN", "PYPL", "DIS",
            "NKE", "ORCL", "IBM", "GE", "CAT", "BA", "UPS", "F", "GM", "SBUX"
    );

    public static final List<String> ARGENTINA_TICKERS = List.of(
            "GGAL", "YPF", "BMA", "PAM", "TEO", "CRESY", "IRS", "LOMA"
    );

    public static List<String> tickersFor(Market market) {
        return switch (market) {
            case US -> US_TICKERS;
            case ARGENTINA -> ARGENTINA_TICKERS;
        };
    }

    /**
     * Company names for every ticker above — used to show a full name on
     * ticker hover across the app, and to power the favorites-input
     * autocomplete (StockService.getUniverse — deliberately market-agnostic,
     * since a user's favorites aren't segmented by market). A ticker a user
     * favorites outside both lists (favorites accepts free-text) won't have
     * an entry here, and getQuotes()'s own {@code name} (from the live
     * provider, when available) is the fallback for those.
     */
    public static final Map<String, String> COMPANY_NAMES = Map.ofEntries(
            Map.entry("AAPL", "Apple Inc."),
            Map.entry("MSFT", "Microsoft Corporation"),
            Map.entry("GOOGL", "Alphabet Inc."),
            Map.entry("AMZN", "Amazon.com, Inc."),
            Map.entry("NVDA", "NVIDIA Corporation"),
            Map.entry("META", "Meta Platforms, Inc."),
            Map.entry("TSLA", "Tesla, Inc."),
            Map.entry("AVGO", "Broadcom Inc."),
            Map.entry("JPM", "JPMorgan Chase & Co."),
            Map.entry("V", "Visa Inc."),
            Map.entry("UNH", "UnitedHealth Group Incorporated"),
            Map.entry("XOM", "Exxon Mobil Corporation"),
            Map.entry("JNJ", "Johnson & Johnson"),
            Map.entry("WMT", "Walmart Inc."),
            Map.entry("PG", "The Procter & Gamble Company"),
            Map.entry("MA", "Mastercard Incorporated"),
            Map.entry("HD", "The Home Depot, Inc."),
            Map.entry("CVX", "Chevron Corporation"),
            Map.entry("MRK", "Merck & Co., Inc."),
            Map.entry("ABBV", "AbbVie Inc."),
            Map.entry("KO", "The Coca-Cola Company"),
            Map.entry("PEP", "PepsiCo, Inc."),
            Map.entry("BAC", "Bank of America Corporation"),
            Map.entry("COST", "Costco Wholesale Corporation"),
            Map.entry("TMO", "Thermo Fisher Scientific Inc."),
            Map.entry("MCD", "McDonald's Corporation"),
            Map.entry("CSCO", "Cisco Systems, Inc."),
            Map.entry("ACN", "Accenture plc"),
            Map.entry("ABT", "Abbott Laboratories"),
            Map.entry("DHR", "Danaher Corporation"),
            Map.entry("LIN", "Linde plc"),
            Map.entry("ADBE", "Adobe Inc."),
            Map.entry("CRM", "Salesforce, Inc."),
            Map.entry("NFLX", "Netflix, Inc."),
            Map.entry("AMD", "Advanced Micro Devices, Inc."),
            Map.entry("INTC", "Intel Corporation"),
            Map.entry("QCOM", "QUALCOMM Incorporated"),
            Map.entry("TXN", "Texas Instruments Incorporated"),
            Map.entry("PYPL", "PayPal Holdings, Inc."),
            Map.entry("DIS", "The Walt Disney Company"),
            Map.entry("NKE", "NIKE, Inc."),
            Map.entry("ORCL", "Oracle Corporation"),
            Map.entry("IBM", "International Business Machines Corporation"),
            Map.entry("GE", "General Electric Company"),
            Map.entry("CAT", "Caterpillar Inc."),
            Map.entry("BA", "The Boeing Company"),
            Map.entry("UPS", "United Parcel Service, Inc."),
            Map.entry("F", "Ford Motor Company"),
            Map.entry("GM", "General Motors Company"),
            Map.entry("SBUX", "Starbucks Corporation"),
            // Argentina (US-listed ADRs) — see class Javadoc.
            Map.entry("GGAL", "Grupo Financiero Galicia S.A."),
            Map.entry("YPF", "YPF Sociedad Anónima"),
            Map.entry("BMA", "Banco Macro S.A."),
            Map.entry("PAM", "Pampa Energía S.A."),
            Map.entry("TEO", "Telecom Argentina S.A."),
            Map.entry("CRESY", "Cresud S.A.C.I.F. y A."),
            Map.entry("IRS", "IRSA Inversiones y Representaciones S.A."),
            Map.entry("LOMA", "Loma Negra Compañía Industrial Argentina S.A.")
    );

    private StockUniverse() {
    }
}
