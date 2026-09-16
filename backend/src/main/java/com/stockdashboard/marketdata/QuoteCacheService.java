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
 */
@Service
public class QuoteCacheService {

    private final MarketDataClient marketDataClient;
    private final long cacheTtlSeconds;
    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();

    public QuoteCacheService(
            MarketDataClient marketDataClient,
            @Value("${app.marketdata.cache-ttl-seconds:60}") long cacheTtlSeconds
    ) {
        this.marketDataClient = marketDataClient;
        this.cacheTtlSeconds = cacheTtlSeconds;
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
            Map<String, Quote> fetched = marketDataClient.getQuotes(stale);
            fetched.forEach((ticker, quote) -> {
                cache.put(ticker, new CachedQuote(quote, now));
                fresh.put(ticker, quote);
            });
        }
        return fresh;
    }
}
