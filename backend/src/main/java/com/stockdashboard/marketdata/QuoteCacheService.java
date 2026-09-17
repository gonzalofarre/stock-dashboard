package com.stockdashboard.marketdata;

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
 * A short-TTL in-memory cache in front of whichever MarketDataClient is
 * active — so N users all favoriting AAPL doesn't mean N provider calls.
 *
 * Deliberately in-memory rather than the shared `stock_quote_cache` DB table
 * sketched in the original architecture proposal: this app runs as one
 * instance for now, and an in-process cache is simpler and just as effective
 * at the one thing that actually matters here (not re-fetching the same
 * ticker on every request). Move it to the DB if/when this ever needs to
 * survive restarts or run across multiple instances.
 *
 * {@code maxStaleFetchPerCall} exists for a real constraint, not a made-up
 * one: Twelve Data's free tier caps requests at 8 API credits/minute
 * (confirmed against a real key — a single request pricing 50 symbols was
 * rejected with "52 API credits were used, with the current limit being 8"),
 * and "most active"/"strategy" both want quotes for the full ~50-ticker
 * universe. Rather than one request blowing the whole minute's budget (and
 * the whole batch failing), a call only ever fetches up to this many stale
 * tickers — the rest stay stale and get picked up by a LATER call (either
 * organic traffic, or MarketDataCacheWarmer's scheduled tick), so the cache
 * fills in gradually instead of failing outright. Harmless no-op for the
 * mock provider, which has no such limit — just set generously high there.
 */
@Service
public class QuoteCacheService {

    private final MarketDataClient marketDataClient;
    private final long cacheTtlSeconds;
    private final int maxStaleFetchPerCall;
    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();

    public QuoteCacheService(
            MarketDataClient marketDataClient,
            @Value("${app.marketdata.cache-ttl-seconds:60}") long cacheTtlSeconds,
            @Value("${app.marketdata.max-stale-fetch-per-call:100}") int maxStaleFetchPerCall
    ) {
        this.marketDataClient = marketDataClient;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.maxStaleFetchPerCall = maxStaleFetchPerCall;
    }

    private record CachedQuote(Quote quote, Instant fetchedAt) {
    }

    public Map<String, Quote> getQuotes(List<String> tickers) {
        Instant now = Instant.now();
        Map<String, Quote> fresh = new HashMap<>();
        List<String> stale = tickers.stream()
                .filter(ticker -> {
                    CachedQuote cached = cache.get(ticker);
                    boolean isFresh = cached != null
                            && Duration.between(cached.fetchedAt(), now).getSeconds() < cacheTtlSeconds;
                    if (isFresh) {
                        fresh.put(ticker, cached.quote());
                    }
                    return !isFresh;
                })
                .collect(Collectors.toList());

        if (!stale.isEmpty()) {
            List<String> toFetch = stale.size() > maxStaleFetchPerCall ? stale.subList(0, maxStaleFetchPerCall) : stale;
            Map<String, Quote> fetched = marketDataClient.getQuotes(toFetch);
            fetched.forEach((ticker, quote) -> {
                cache.put(ticker, new CachedQuote(quote, now));
                fresh.put(ticker, quote);
            });
        }
        return fresh;
    }
}
