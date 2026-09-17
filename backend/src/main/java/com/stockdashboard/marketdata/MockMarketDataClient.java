package com.stockdashboard.marketdata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Default market-data source for local development: fabricates a plausible-
 * looking but entirely fake quote per ticker, so favorites/dashboard UI work
 * end to end before a real Twelve Data key exists. The base price is stable
 * per ticker (derived from its hashcode) so the same symbol doesn't jump to
 * a wildly different price on every request — only a small "movement" jitters
 * each call, to look alive in the UI.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "mock", matchIfMissing = true)
public class MockMarketDataClient implements MarketDataClient {

    @Override
    public Map<String, Quote> getQuotes(List<String> tickers) {
        log.info("[MOCK MARKET DATA] Generating fake quotes for {} — set TWELVE_DATA_API_KEY for real data.", tickers);
        Map<String, Quote> result = new HashMap<>();
        for (String ticker : tickers) {
            Random random = new Random(ticker.hashCode());
            double basePrice = 20 + random.nextDouble() * 480; // a stable-per-ticker price between $20 and $500
            long baseVolume = 500_000 + (long) (random.nextDouble() * 49_500_000); // stable-per-ticker "typical" volume
            double changePercent = (new Random().nextDouble() - 0.5) * 6; // +/- 3%, fresh jitter every call
            long volumeJitter = (long) ((new Random().nextDouble() - 0.5) * baseVolume * 0.4); // today's volume wobbles around the typical one
            result.put(ticker, new Quote(
                    ticker,
                    BigDecimal.valueOf(basePrice).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(changePercent).setScale(2, RoundingMode.HALF_UP),
                    Math.max(0, baseVolume + volumeJitter),
                    Instant.now()
            ));
        }
        return result;
    }
}
