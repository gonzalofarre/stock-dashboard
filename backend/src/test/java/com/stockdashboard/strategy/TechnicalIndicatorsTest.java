package com.stockdashboard.strategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TechnicalIndicatorsTest {

    private List<BigDecimal> closes(double... values) {
        return Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
    }

    @Test
    void emaIsNullBeforeThePeriodSeeds() {
        List<BigDecimal> ema = TechnicalIndicators.ema(closes(1, 2, 3, 4, 5), 3);

        assertThat(ema.get(0)).isNull();
        assertThat(ema.get(1)).isNull();
        assertThat(ema.get(2)).isNotNull();
    }

    @Test
    void seedsWithTheSimpleAverageThenAppliesTheEmaFormula() {
        // period=3 over [1,2,3,4,5]: seed = avg(1,2,3) = 2 at index 2,
        // then k = 2/(3+1) = 0.5 -> index3 = (4-2)*0.5+2 = 3, index4 = (5-3)*0.5+3 = 4
        List<BigDecimal> ema = TechnicalIndicators.ema(closes(1, 2, 3, 4, 5), 3);

        assertThat(ema.get(2)).isEqualByComparingTo("2");
        assertThat(ema.get(3)).isEqualByComparingTo("3");
        assertThat(ema.get(4)).isEqualByComparingTo("4");
    }

    @Test
    void tooFewValuesForThePeriodYieldsAllNulls() {
        List<BigDecimal> ema = TechnicalIndicators.ema(closes(1, 2), 3);

        assertThat(ema).containsOnlyNulls();
    }

    @Test
    void emaSkipsLeadingNullsInsteadOfBreaking() {
        // Simulates feeding a MACD line (null until its slow EMA seeds) into ema().
        List<BigDecimal> withLeadingNulls = Arrays.asList(
                null, null,
                BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(5)
        );

        List<BigDecimal> ema = TechnicalIndicators.ema(withLeadingNulls, 3);

        // start=2, period=3 -> seed at index 4 = avg(1,2,3) = 2
        assertThat(ema.get(4)).isEqualByComparingTo("2");
        assertThat(ema.get(5)).isEqualByComparingTo("3");
        assertThat(ema.get(6)).isEqualByComparingTo("4");
    }

    @Test
    void differenceIsNullWhereEitherInputIsNull() {
        List<BigDecimal> fast = Arrays.asList(null, BigDecimal.valueOf(5), BigDecimal.valueOf(6));
        List<BigDecimal> slow = Arrays.asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), null);

        List<BigDecimal> diff = TechnicalIndicators.difference(fast, slow);

        assertThat(diff.get(0)).isNull();
        assertThat(diff.get(1)).isEqualByComparingTo("3");
        assertThat(diff.get(2)).isNull();
    }

    @Test
    void detectsABullishCrossoverAndHowManyBarsAgo() {
        List<BigDecimal> fast = closes(1, 2, 3); // crosses above slow between index 1 and 2
        List<BigDecimal> slow = closes(2, 2, 2);

        var crossover = TechnicalIndicators.detectCrossover(fast, slow, 5);

        assertThat(crossover).isPresent();
        assertThat(crossover.get().direction()).isEqualTo(Direction.BULLISH);
        assertThat(crossover.get().barsAgo()).isZero(); // the crossover is between the last two bars
    }

    @Test
    void detectsABearishCrossover() {
        List<BigDecimal> fast = closes(3, 2, 1);
        List<BigDecimal> slow = closes(2, 2, 2);

        var crossover = TechnicalIndicators.detectCrossover(fast, slow, 5);

        assertThat(crossover).isPresent();
        assertThat(crossover.get().direction()).isEqualTo(Direction.BEARISH);
    }

    @Test
    void noCrossoverWhenOneSeriesStaysOnTopTheWholeTime() {
        List<BigDecimal> fast = closes(5, 6, 7, 8);
        List<BigDecimal> slow = closes(1, 1, 1, 1);

        assertThat(TechnicalIndicators.detectCrossover(fast, slow, 5)).isEmpty();
    }

    @Test
    void ignoresACrossoverOutsideTheLookbackWindow() {
        // Crosses between index 0 and 1, then stays above — with lookback=1
        // only the last transition (index 3->4, no cross) is inspected.
        List<BigDecimal> fast = closes(1, 3, 4, 5, 6);
        List<BigDecimal> slow = closes(2, 2, 2, 2, 2);

        assertThat(TechnicalIndicators.detectCrossover(fast, slow, 1)).isEmpty();
        assertThat(TechnicalIndicators.detectCrossover(fast, slow, 5)).isPresent();
    }
}
