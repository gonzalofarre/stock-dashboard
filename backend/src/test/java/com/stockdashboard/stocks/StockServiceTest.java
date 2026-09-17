package com.stockdashboard.stocks;

import com.stockdashboard.common.ApiException;
import com.stockdashboard.marketdata.Quote;
import com.stockdashboard.marketdata.QuoteCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private QuoteCacheService quoteCacheService;

    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockService(quoteCacheService);
    }

    @Test
    void returnsTheQuoteWhenAvailable() {
        when(quoteCacheService.getQuotes(java.util.List.of("AAPL"))).thenReturn(Map.of(
                "AAPL", new Quote("AAPL", BigDecimal.valueOf(258.42), BigDecimal.valueOf(1.24), 61_200_000L, null, Instant.now())
        ));

        StockQuoteResponse response = stockService.getQuote("aapl");

        assertThat(response.ticker()).isEqualTo("AAPL"); // uppercased
        assertThat(response.price()).isEqualByComparingTo("258.42");
        assertThat(response.changePercent()).isEqualByComparingTo("1.24");
        assertThat(response.volume()).isEqualTo(61_200_000L);
        assertThat(response.quoteAvailable()).isTrue();
    }

    @Test
    void marksTheQuoteUnavailableInsteadOfFailingWhenTheProviderHasNoData() {
        when(quoteCacheService.getQuotes(java.util.List.of("ZZZZ"))).thenReturn(Map.of());

        StockQuoteResponse response = stockService.getQuote("ZZZZ");

        assertThat(response.quoteAvailable()).isFalse();
        assertThat(response.price()).isNull();
        assertThat(response.changePercent()).isNull();
        assertThat(response.volume()).isNull();
    }

    @Test
    void rejectsAnInvalidTickerFormat() {
        assertThatThrownBy(() -> stockService.getQuote("not a ticker!"))
                .isInstanceOf(ApiException.class);
    }
}
