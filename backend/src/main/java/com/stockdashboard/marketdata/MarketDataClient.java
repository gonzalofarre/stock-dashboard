package com.stockdashboard.marketdata;

import java.util.List;
import java.util.Map;

/**
 * Abstraction over whichever market-data provider is actually configured —
 * lets the rest of the app (favorites, and later the suggestion blocks) stay
 * provider-agnostic, and lets local dev work without a real API key.
 */
public interface MarketDataClient {
    /**
     * Returns whatever quotes it could successfully fetch, keyed by ticker.
     * A ticker that failed (unknown symbol, provider error) is simply absent
     * from the result rather than throwing — one bad ticker in a batch
     * shouldn't take down the whole favorites list.
     */
    Map<String, Quote> getQuotes(List<String> tickers);
}
