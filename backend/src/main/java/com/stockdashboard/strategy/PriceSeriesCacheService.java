package com.stockdashboard.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The strategy-signal equivalent of marketdata/QuoteCacheService — same
 * rate-limit-driven shape (a stale-tickers fetch capped per call, so the
 * ~50-ticker universe fills in gradually instead of one call blowing
 * Twelve Data's free-tier 8-credits/minute budget), same in-memory-only
 * choice, just caching a Bar series per ticker instead of a Quote. See that
 * class's Javadoc for the full reasoning — kept separate rather than a
 * generic shared cache because Quote and price-series data have different
 * shapes and StrategyService always requests the same bar count, so keying
 * by ticker alone (no bar-count dimension) is simplest here.
 */
@Service
public class PriceSeriesCacheService {

    private final PriceSeriesClient priceSeriesClient;
    private final long cacheTtlSeconds;
    private final int maxStaleFetchPerCall;
    private final Map<String, CachedSeries> cache = new ConcurrentHashMap<>();

    public PriceSeriesCacheService(
            PriceSeriesClient priceSeriesClient,
            @Value("${app.strategy.cache-ttl-seconds:60}") long cacheTtlSeconds,
            @Value("${app.strategy.max-stale-fetch-per-call:100}") int maxStaleFetchPerCall
    ) {
        this.priceSeriesClient = priceSeriesClient;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.maxStaleFetchPerCall = maxStaleFetchPerCall;
    }

    private record CachedSeries(List<Bar> bars, Instant fetchedAt) {
    }

    public Map<String, List<Bar>> getIntradaySeries(List<String> tickers, int barCount) {
        Instant now = Instant.now();
        Map<String, List<Bar>> fresh = new HashMap<>();
        List<String> stale = tickers.stream()
                .filter(ticker -> {
                    CachedSeries cached = cache.get(ticker);
                    boolean isFresh = cached != null
                            && Duration.between(cached.fetchedAt(), now).getSeconds() < cacheTtlSeconds;
                    if (isFresh) {
                        fresh.put(ticker, cached.bars());
                    }
                    return !isFresh;
                })
                .collect(Collectors.toList());

        if (!stale.isEmpty()) {
            List<String> toFetch = stale.size() > maxStaleFetchPerCall ? stale.subList(0, maxStaleFetchPerCall) : stale;
            Map<String, List<Bar>> fetched = priceSeriesClient.getIntradaySeries(toFetch, barCount);
            fetched.forEach((ticker, bars) -> {
                cache.put(ticker, new CachedSeries(bars, now));
                fresh.put(ticker, bars);
            });
        }
        return fresh;
    }
}
