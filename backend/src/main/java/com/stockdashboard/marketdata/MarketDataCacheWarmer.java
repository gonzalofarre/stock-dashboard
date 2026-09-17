package com.stockdashboard.marketdata;

import com.stockdashboard.strategy.PriceSeriesCacheService;
import com.stockdashboard.strategy.StrategyService;
import com.stockdashboard.suggestions.StockUniverse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives QuoteCacheService and PriceSeriesCacheService's gradual warm-up so
 * "most active", earnings-adjacent quotes, and "strategy" have a full
 * ~50-ticker cache ready without any one request having to fetch all 50 at
 * once (see QuoteCacheService's Javadoc for why that matters on Twelve
 * Data's free tier).
 *
 * Ticks alternate between warming quotes and warming price series rather
 * than doing both every tick: Twelve Data's 8-credits/minute cap is shared
 * across endpoints (confirmed against a real key — a single 50-symbol
 * /time_series request was rejected the same way a 50-symbol /quote request
 * would be), so running both warmers in the same minute would still blow the
 * budget even though each individually respects it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataCacheWarmer {

    private final QuoteCacheService quoteCacheService;
    private final PriceSeriesCacheService priceSeriesCacheService;

    private boolean warmQuotesNext = true;

    @Scheduled(initialDelayString = "PT5S", fixedDelayString = "PT65S")
    public void warm() {
        if (warmQuotesNext) {
            quoteCacheService.getQuotes(StockUniverse.LIQUID_US_STOCKS);
        } else {
            priceSeriesCacheService.getIntradaySeries(StockUniverse.LIQUID_US_STOCKS, StrategyService.BARS_REQUESTED);
        }
        warmQuotesNext = !warmQuotesNext;
    }
}
