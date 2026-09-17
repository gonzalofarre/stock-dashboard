package com.stockdashboard.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Default price-series source for local development: a deterministic
 * per-ticker random walk. Deliberately seeded ONLY from the ticker (no fresh
 * jitter on every call, unlike MockMarketDataClient's quotes) — a signal
 * scanner that flickers a different crossover in and out on every request
 * would look broken, so the same walk (and therefore the same crossovers)
 * comes back every time until the app restarts.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.strategy.provider", havingValue = "mock", matchIfMissing = true)
public class MockPriceSeriesClient implements PriceSeriesClient {

    private static final Duration BAR_INTERVAL = Duration.ofMinutes(15);

    @Override
    public Map<String, List<Bar>> getIntradaySeries(List<String> tickers, int barCount) {
        log.info("[MOCK STRATEGY DATA] Generating a fake {}-bar intraday series for {} tickers — set "
                + "STRATEGY_PROVIDER=twelvedata (and TWELVE_DATA_API_KEY) for real data.", barCount, tickers.size());
        Instant now = Instant.now();
        Map<String, List<Bar>> result = new HashMap<>();
        for (String ticker : tickers) {
            Random random = new Random(ticker.hashCode());
            double price = 20 + random.nextDouble() * 480;
            List<Bar> bars = new ArrayList<>(barCount);
            for (int i = 0; i < barCount; i++) {
                price = Math.max(1.0, price * (1 + random.nextGaussian() * 0.004));
                Instant time = now.minus(BAR_INTERVAL.multipliedBy((long) (barCount - 1 - i)));
                bars.add(new Bar(time, BigDecimal.valueOf(price).setScale(4, RoundingMode.HALF_UP)));
            }
            result.put(ticker, bars);
        }
        return result;
    }
}
