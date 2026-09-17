package com.stockdashboard.marketdata;

import com.stockdashboard.strategy.PriceSeriesCacheService;
import com.stockdashboard.strategy.StrategyService;
import com.stockdashboard.suggestions.Market;
import com.stockdashboard.suggestions.StockUniverse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives QuoteCacheService and PriceSeriesCacheService's gradual warm-up so
 * "most active", earnings-adjacent quotes, and "strategy" have a full cache
 * ready without any one request having to fetch a whole universe at once
 * (see QuoteCacheService's Javadoc for why that matters on Twelve Data's
 * free tier).
 *
 * Cycles through 4 states — US quotes, US series, Argentina quotes,
 * Argentina series — one per tick, rather than doing everything every tick:
 * Twelve Data's 8-credits/minute cap is shared across endpoints (confirmed
 * against a real key — a single 50-symbol /time_series request was rejected
 * the same way a 50-symbol /quote request would be), so warming everything
 * at once would still blow the per-minute budget even though each
 * individual call respects it.
 *
 * The tick interval is deliberately conservative, not just per-minute-safe:
 * this free tier also caps at 800 credits/DAY, discovered the hard way when
 * an earlier version of this class (ticking every 65s) burned through a
 * whole day's budget in a few hours just by being left running, with nobody
 * even using the app. At this cadence (4 states x 20 min = each state ticks
 * every 80 min) the daily cost is (1440/20)*8 = 576 credits/day worst case,
 * leaving headroom for actual live traffic. The tradeoff: a cold cache can
 * take hours to fully warm through the background ticks alone — organic
 * request traffic (QuoteCacheService/PriceSeriesCacheService's own per-call
 * stale-fetch) fills in faster for whatever's actually being looked at.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketDataCacheWarmer {

    private final QuoteCacheService quoteCacheService;
    private final PriceSeriesCacheService priceSeriesCacheService;

    private int tick = 0;

    @Scheduled(initialDelayString = "PT10S", fixedDelayString = "PT20M")
    public void warm() {
        Market market = tick < 2 ? Market.US : Market.ARGENTINA;
        boolean warmQuotes = tick % 2 == 0;
        var tickers = StockUniverse.tickersFor(market);

        if (warmQuotes) {
            quoteCacheService.getQuotes(tickers);
        } else {
            priceSeriesCacheService.getIntradaySeries(tickers, StrategyService.BARS_REQUESTED);
        }
        tick = (tick + 1) % 4;
    }
}
