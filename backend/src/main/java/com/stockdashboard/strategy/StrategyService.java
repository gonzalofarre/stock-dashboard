package com.stockdashboard.strategy;

import com.stockdashboard.suggestions.StockUniverse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Sugerencia 3: intraday EMA9/21 and MACD(12,26,9) crossovers over the same
 * curated universe as "most active" (see StockUniverse). Extensible by
 * design — adding an indicator later (RSI, Bollinger) means one more
 * detect*() call inside detectSignalsFor, not a rewrite.
 *
 * Unlike "most active" or "earnings", this never pads results to hit the
 * requested count: some scans will genuinely find fewer than 5 fresh
 * crossovers across the whole universe, and returning fewer real signals
 * beats fabricating ones that didn't happen.
 */
@Service
@RequiredArgsConstructor
public class StrategyService {

    private static final int MIN_RESULTS = 5;
    private static final int MAX_RESULTS = 10;

    private static final int BARS_REQUESTED = 50;
    private static final int LOOKBACK_BARS = 8;
    private static final int BAR_INTERVAL_MINUTES = 15;

    private static final int EMA_FAST = 9;
    private static final int EMA_SLOW = 21;
    private static final int MACD_FAST = 12;
    private static final int MACD_SLOW = 26;
    private static final int MACD_SIGNAL = 9;

    private final PriceSeriesClient priceSeriesClient;

    public List<StrategySignalResponse> getSignals(int requestedLimit) {
        int limit = Math.clamp(requestedLimit, MIN_RESULTS, MAX_RESULTS);

        Map<String, List<Bar>> series = priceSeriesClient.getIntradaySeries(StockUniverse.LIQUID_US_STOCKS, BARS_REQUESTED);

        List<StrategySignalResponse> signals = new ArrayList<>();
        for (var entry : series.entrySet()) {
            signals.addAll(detectSignalsFor(entry.getKey(), entry.getValue()));
        }

        return signals.stream()
                .sorted(Comparator.comparingInt(StrategySignalResponse::minutesAgo)
                        .thenComparing(StrategySignalResponse::ticker))
                .limit(limit)
                .toList();
    }

    private List<StrategySignalResponse> detectSignalsFor(String ticker, List<Bar> bars) {
        if (bars.size() < BARS_REQUESTED) {
            return List.of();
        }
        List<BigDecimal> closes = bars.stream().map(Bar::close).toList();
        List<StrategySignalResponse> result = new ArrayList<>();

        List<BigDecimal> emaFast = TechnicalIndicators.ema(closes, EMA_FAST);
        List<BigDecimal> emaSlow = TechnicalIndicators.ema(closes, EMA_SLOW);
        TechnicalIndicators.detectCrossover(emaFast, emaSlow, LOOKBACK_BARS)
                .ifPresent(c -> result.add(toResponse(ticker, SignalType.EMA_CROSSOVER, c)));

        List<BigDecimal> macdLine = TechnicalIndicators.difference(
                TechnicalIndicators.ema(closes, MACD_FAST), TechnicalIndicators.ema(closes, MACD_SLOW));
        List<BigDecimal> signalLine = TechnicalIndicators.ema(macdLine, MACD_SIGNAL);
        TechnicalIndicators.detectCrossover(macdLine, signalLine, LOOKBACK_BARS)
                .ifPresent(c -> result.add(toResponse(ticker, SignalType.MACD_CROSSOVER, c)));

        return result;
    }

    private StrategySignalResponse toResponse(String ticker, SignalType type, TechnicalIndicators.Crossover crossover) {
        return new StrategySignalResponse(ticker, type, crossover.direction(), crossover.barsAgo() * BAR_INTERVAL_MINUTES);
    }
}
