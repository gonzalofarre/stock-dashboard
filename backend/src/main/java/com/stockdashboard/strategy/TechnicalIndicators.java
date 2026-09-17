package com.stockdashboard.strategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Small, dependency-free indicator math over a close-price series. Every
 * returned series is the SAME LENGTH as its input, index-aligned to it —
 * entries before an indicator has enough data to seed are {@code null}
 * rather than the list being shorter, so two indicators (e.g. EMA9 and
 * EMA21) can always be compared at the same index without extra bookkeeping.
 */
public final class TechnicalIndicators {

    private TechnicalIndicators() {
    }

    /**
     * Exponential moving average, seeded with a plain average of the first
     * {@code period} non-null values (leading nulls in {@code series} — e.g.
     * feeding a MACD line, itself null until its slow EMA seeds — are
     * skipped rather than breaking the calculation).
     */
    public static List<BigDecimal> ema(List<BigDecimal> series, int period) {
        int n = series.size();
        BigDecimal[] result = new BigDecimal[n];

        int start = 0;
        while (start < n && series.get(start) == null) {
            start++;
        }
        if (n - start < period) {
            return Arrays.asList(result);
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = start; i < start + period; i++) {
            sum = sum.add(series.get(i));
        }
        BigDecimal emaPrev = sum.divide(BigDecimal.valueOf(period), MathContext.DECIMAL64);
        int seedIndex = start + period - 1;
        result[seedIndex] = emaPrev;

        BigDecimal k = BigDecimal.valueOf(2.0 / (period + 1));
        for (int i = seedIndex + 1; i < n; i++) {
            BigDecimal emaCur = series.get(i).subtract(emaPrev).multiply(k).add(emaPrev);
            result[i] = emaCur;
            emaPrev = emaCur;
        }
        return Arrays.asList(result);
    }

    /** {@code fastEma - slowEma} at every index (null wherever either input is null). */
    public static List<BigDecimal> difference(List<BigDecimal> fastEma, List<BigDecimal> slowEma) {
        BigDecimal[] result = new BigDecimal[slowEma.size()];
        for (int i = 0; i < slowEma.size(); i++) {
            if (fastEma.get(i) != null && slowEma.get(i) != null) {
                result[i] = fastEma.get(i).subtract(slowEma.get(i));
            }
        }
        return Arrays.asList(result);
    }

    /**
     * Scans backwards from the last bar, over at most {@code lookbackBars}
     * transitions, for the most recent point where {@code fast} crossed
     * {@code slow}. Empty if no crossover happened in that window (or either
     * series doesn't have enough data there yet).
     */
    public static Optional<Crossover> detectCrossover(List<BigDecimal> fast, List<BigDecimal> slow, int lookbackBars) {
        int n = fast.size();
        int earliest = Math.max(1, n - lookbackBars);
        for (int i = n - 1; i >= earliest; i--) {
            BigDecimal fastPrev = fast.get(i - 1);
            BigDecimal slowPrev = slow.get(i - 1);
            BigDecimal fastCur = fast.get(i);
            BigDecimal slowCur = slow.get(i);
            if (fastPrev == null || slowPrev == null || fastCur == null || slowCur == null) {
                continue;
            }
            if (fastPrev.compareTo(slowPrev) <= 0 && fastCur.compareTo(slowCur) > 0) {
                return Optional.of(new Crossover(Direction.BULLISH, n - 1 - i));
            }
            if (fastPrev.compareTo(slowPrev) >= 0 && fastCur.compareTo(slowCur) < 0) {
                return Optional.of(new Crossover(Direction.BEARISH, n - 1 - i));
            }
        }
        return Optional.empty();
    }

    /** {@code barsAgo}: 0 means the crossover is between the last two bars. */
    public record Crossover(Direction direction, int barsAgo) {
    }
}
