package com.stockdashboard.strategy;

import java.util.List;
import java.util.Map;

public interface PriceSeriesClient {

    /**
     * Up to {@code barCount} most recent intraday bars per ticker, oldest
     * first. A ticker missing from the result (or with fewer than
     * {@code barCount} bars) is simply skipped by StrategyService — not
     * every provider will have full history for every symbol.
     */
    Map<String, List<Bar>> getIntradaySeries(List<String> tickers, int barCount);
}
