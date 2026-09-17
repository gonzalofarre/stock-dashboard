package com.stockdashboard.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StrategyServiceTest {

    @Mock
    private PriceSeriesClient priceSeriesClient;

    private StrategyService strategyService;

    @BeforeEach
    void setUp() {
        strategyService = new StrategyService(priceSeriesClient);
    }

    /**
     * Flat at 100 up to (not including) jumpIndex, then flat at
     * {@code 100 * jumpMultiplier} from jumpIndex on — a step function. Since
     * both EMAs start exactly equal (fully converged on the flat price), ANY
     * upward step makes the faster EMA (higher weight) pull further above
     * the slower one than the slower one moves, guaranteeing a detected
     * bullish crossover at exactly jumpIndex — precise and easy to reason
     * about, unlike a multi-bar ramp where the exact crossing bar depends on
     * EMA dynamics.
     */
    private List<Bar> flatWithStepAt(int totalBars, int jumpIndex, double jumpMultiplier) {
        List<Bar> bars = new ArrayList<>();
        double basePrice = 100.0;
        double steppedPrice = basePrice * jumpMultiplier;
        Instant now = Instant.now();
        for (int i = 0; i < totalBars; i++) {
            double price = i < jumpIndex ? basePrice : steppedPrice;
            bars.add(new Bar(now.minusSeconds((long) (totalBars - i) * 900), BigDecimal.valueOf(price)));
        }
        return bars;
    }

    @Test
    void detectsABullishEmaCrossoverRightAfterAStepUp() {
        // Step 2 bars before the end (index 47 of 50) -> well inside the 8-bar lookback window.
        when(priceSeriesClient.getIntradaySeries(any(), anyInt()))
                .thenReturn(Map.of("UP", flatWithStepAt(50, 47, 1.2)));

        List<StrategySignalResponse> result = strategyService.getSignals(10);

        assertThat(result).anySatisfy(s -> {
            assertThat(s.ticker()).isEqualTo("UP");
            assertThat(s.type()).isEqualTo(SignalType.EMA_CROSSOVER);
            assertThat(s.direction()).isEqualTo(Direction.BULLISH);
        });
    }

    @Test
    void aFlatSeriesProducesNoSignals() {
        when(priceSeriesClient.getIntradaySeries(any(), anyInt()))
                .thenReturn(Map.of("FLAT", flatWithStepAt(50, 999, 1.0)));

        assertThat(strategyService.getSignals(10)).isEmpty();
    }

    @Test
    void skipsTickersWithFewerBarsThanRequired() {
        when(priceSeriesClient.getIntradaySeries(any(), anyInt()))
                .thenReturn(Map.of("SHORT", flatWithStepAt(30, 20, 1.2)));

        assertThat(strategyService.getSignals(10)).isEmpty();
    }

    @Test
    void clampsTheLimitToThe5To10Range() {
        Map<String, List<Bar>> many = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            many.put("T" + i, flatWithStepAt(50, 47, 1.2));
        }
        when(priceSeriesClient.getIntradaySeries(any(), anyInt())).thenReturn(many);

        assertThat(strategyService.getSignals(1)).hasSize(5); // below min -> clamped up to 5
        assertThat(strategyService.getSignals(50)).hasSize(10); // above max -> clamped down to 10
    }

    @Test
    void ordersSignalsByHowRecentTheCrossoverWas() {
        Map<String, List<Bar>> many = new HashMap<>();
        many.put("A", flatWithStepAt(50, 43, 1.2)); // crosses 6 bars ago
        many.put("B", flatWithStepAt(50, 48, 1.2)); // crosses 1 bar ago
        when(priceSeriesClient.getIntradaySeries(any(), anyInt())).thenReturn(many);

        List<StrategySignalResponse> result = strategyService.getSignals(10);

        assertThat(result).isNotEmpty();
        assertThat(result).isSortedAccordingTo(Comparator.comparingInt(StrategySignalResponse::minutesAgo));
        assertThat(result.get(0).ticker()).isEqualTo("B");
        assertThat(result.get(result.size() - 1).ticker()).isEqualTo("A");
    }
}
