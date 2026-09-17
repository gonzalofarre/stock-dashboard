package com.stockdashboard.strategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceSeriesCacheServiceTest {

    @Mock
    private PriceSeriesClient priceSeriesClient;

    private List<Bar> oneBar() {
        return List.of(new Bar(Instant.now(), BigDecimal.TEN));
    }

    @Test
    void aSecondCallWithinTheTtlDoesNotHitTheUnderlyingClientAgain() {
        PriceSeriesCacheService cacheService = new PriceSeriesCacheService(priceSeriesClient, 60, 100);
        when(priceSeriesClient.getIntradaySeries(List.of("AAPL"), 50)).thenReturn(Map.of("AAPL", oneBar()));

        cacheService.getIntradaySeries(List.of("AAPL"), 50);
        cacheService.getIntradaySeries(List.of("AAPL"), 50);

        verify(priceSeriesClient, times(1)).getIntradaySeries(List.of("AAPL"), 50);
    }

    @Test
    void aCallAfterTheTtlExpiresHitsTheUnderlyingClientAgain() throws InterruptedException {
        PriceSeriesCacheService cacheService = new PriceSeriesCacheService(priceSeriesClient, 0, 100); // effectively no caching
        when(priceSeriesClient.getIntradaySeries(List.of("AAPL"), 50)).thenReturn(Map.of("AAPL", oneBar()));

        cacheService.getIntradaySeries(List.of("AAPL"), 50);
        Thread.sleep(5);
        cacheService.getIntradaySeries(List.of("AAPL"), 50);

        verify(priceSeriesClient, times(2)).getIntradaySeries(List.of("AAPL"), 50);
    }

    @Test
    void neverFetchesMoreThanMaxStaleFetchPerCallInOneUnderlyingRequest() {
        PriceSeriesCacheService cacheService = new PriceSeriesCacheService(priceSeriesClient, 60, 2);
        List<String> tickers = List.of("AAPL", "MSFT", "GOOGL", "AMZN"); // 4 stale, cap is 2
        when(priceSeriesClient.getIntradaySeries(anyList(), anyInt())).thenReturn(Map.of());

        cacheService.getIntradaySeries(tickers, 50);

        verify(priceSeriesClient).getIntradaySeries(List.of("AAPL", "MSFT"), 50);
        verify(priceSeriesClient, never()).getIntradaySeries(tickers, 50);
    }
}
