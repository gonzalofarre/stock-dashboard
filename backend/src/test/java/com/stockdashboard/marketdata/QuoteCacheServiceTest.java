package com.stockdashboard.marketdata;

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
class QuoteCacheServiceTest {

    @Mock
    private MarketDataClient marketDataClient;

    @Test
    void aSecondCallWithinTheTtlDoesNotHitTheUnderlyingClientAgain() {
        QuoteCacheService cacheService = new QuoteCacheService(marketDataClient, 60, 100);
        when(marketDataClient.getQuotes(List.of("AAPL"))).thenReturn(
                Map.of("AAPL", new Quote("AAPL", BigDecimal.TEN, BigDecimal.ONE, 1_000_000L, null, Instant.now()))
        );

        cacheService.getQuotes(List.of("AAPL"));
        cacheService.getQuotes(List.of("AAPL"));

        verify(marketDataClient, times(1)).getQuotes(List.of("AAPL"));
    }

    @Test
    void aCallAfterTheTtlExpiresHitsTheUnderlyingClientAgain() throws InterruptedException {
        QuoteCacheService cacheService = new QuoteCacheService(marketDataClient, 0, 100); // effectively no caching
        when(marketDataClient.getQuotes(List.of("AAPL"))).thenReturn(
                Map.of("AAPL", new Quote("AAPL", BigDecimal.TEN, BigDecimal.ONE, 1_000_000L, null, Instant.now()))
        );

        cacheService.getQuotes(List.of("AAPL"));
        Thread.sleep(5);
        cacheService.getQuotes(List.of("AAPL"));

        verify(marketDataClient, times(2)).getQuotes(List.of("AAPL"));
    }

    @Test
    void onlyFetchesTheTickersThatAreActuallyStale() {
        QuoteCacheService cacheService = new QuoteCacheService(marketDataClient, 60, 100);
        when(marketDataClient.getQuotes(List.of("AAPL"))).thenReturn(
                Map.of("AAPL", new Quote("AAPL", BigDecimal.TEN, BigDecimal.ONE, 1_000_000L, null, Instant.now()))
        );
        cacheService.getQuotes(List.of("AAPL")); // warms AAPL only

        when(marketDataClient.getQuotes(List.of("MSFT"))).thenReturn(
                Map.of("MSFT", new Quote("MSFT", BigDecimal.valueOf(300), BigDecimal.valueOf(-0.5), 2_000_000L, null, Instant.now()))
        );

        Map<String, Quote> result = cacheService.getQuotes(List.of("AAPL", "MSFT"));

        assertThat(result).containsKeys("AAPL", "MSFT");
        verify(marketDataClient, never()).getQuotes(List.of("AAPL", "MSFT"));
        verify(marketDataClient).getQuotes(List.of("MSFT"));
    }

    @Test
    void neverFetchesMoreThanMaxStaleFetchPerCallInOneUnderlyingRequest() {
        QuoteCacheService cacheService = new QuoteCacheService(marketDataClient, 60, 2);
        List<String> tickers = List.of("AAPL", "MSFT", "GOOGL", "AMZN"); // 4 stale, cap is 2
        when(marketDataClient.getQuotes(anyList())).thenReturn(Map.of());

        cacheService.getQuotes(tickers);

        verify(marketDataClient).getQuotes(List.of("AAPL", "MSFT"));
        verify(marketDataClient, never()).getQuotes(tickers);
    }
}
