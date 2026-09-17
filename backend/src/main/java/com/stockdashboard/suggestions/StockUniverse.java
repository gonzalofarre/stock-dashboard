package com.stockdashboard.suggestions;

import java.util.List;

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

    private StockUniverse() {
    }
}
