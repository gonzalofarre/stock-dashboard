package com.stockdashboard.suggestions;

import java.util.List;
import java.util.Map;

/**
 * A hand-picked list of ~50 large, liquid, well-known US tickers spanning
 * several sectors — used as the pool that "most active" and (later) the
 * intraday-strategy suggestion rank/filter over.
 *
 * This is deliberately NOT a full-market scan: Twelve Data's free tier
 * doesn't give confident access to a dedicated "most active by volume"
 * screener endpoint, so instead of guessing at an unverified endpoint, this
 * reuses the already-tested per-symbol quote infrastructure (see
 * marketdata/QuoteCacheService) over a curated universe. Real "market-wide"
 * coverage would need a different (likely paid) data source — worth
 * revisiting if this universe turns out to be too narrow in practice.
 */
public final class StockUniverse {

    public static final List<String> LIQUID_US_STOCKS = List.of(
            "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA", "AVGO", "JPM", "V",
            "UNH", "XOM", "JNJ", "WMT", "PG", "MA", "HD", "CVX", "MRK", "ABBV",
            "KO", "PEP", "BAC", "COST", "TMO", "MCD", "CSCO", "ACN", "ABT", "DHR",
            "LIN", "ADBE", "CRM", "NFLX", "AMD", "INTC", "QCOM", "TXN", "PYPL", "DIS",
            "NKE", "ORCL", "IBM", "GE", "CAT", "BA", "UPS", "F", "GM", "SBUX"
    );

    /**
     * Company names for the tickers above — used to show a full name on
     * ticker hover across the app. Only covers this curated universe; a
     * ticker a user favorites outside of it (favorites accepts free-text)
     * won't have an entry here, and getQuotes()'s own {@code name} (from the
     * live provider, when available) is the fallback for those.
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
            Map.entry("SBUX", "Starbucks Corporation")
    );

    private StockUniverse() {
    }
}
