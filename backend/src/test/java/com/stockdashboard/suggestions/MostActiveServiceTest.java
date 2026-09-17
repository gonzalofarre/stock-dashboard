package com.stockdashboard.suggestions;

import com.stockdashboard.marketdata.Quote;
import com.stockdashboard.marketdata.QuoteCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MostActiveServiceTest {

    @Mock
    private QuoteCacheService quoteCacheService;

    private MostActiveService mostActiveService;

    @BeforeEach
    void setUp() {
        mostActiveService = new MostActiveService(quoteCacheService);
    }

    private Quote quote(String ticker, long volume) {
        return new Quote(ticker, BigDecimal.TEN, BigDecimal.ONE, volume, null, Instant.now());
    }

    @Test
    void ordersByVolumeDescending() {
        when(quoteCacheService.getQuotes(any())).thenReturn(Map.of(
                "LOW", quote("LOW", 1_000),
                "HIGH", quote("HIGH", 9_000),
                "MID", quote("MID", 5_000)
        ));

        List<MostActiveResponse> result = mostActiveService.getMostActive(5);

        assertThat(result).extracting(MostActiveResponse::ticker).containsExactly("HIGH", "MID", "LOW");
    }

    @Test
    void excludesTickersWithNoVolumeData() {
        when(quoteCacheService.getQuotes(any())).thenReturn(Map.of(
                "GOOD", quote("GOOD", 1_000),
                "NOVOL", new Quote("NOVOL", BigDecimal.TEN, BigDecimal.ONE, null, null, Instant.now())
        ));

        List<MostActiveResponse> result = mostActiveService.getMostActive(10);

        assertThat(result).extracting(MostActiveResponse::ticker).containsExactly("GOOD");
    }

    @Test
    void clampsTheLimitToTheAllowed5To10Range() {
        Map<String, Quote> manyQuotes = new java.util.HashMap<>();
        for (int i = 0; i < 20; i++) {
            manyQuotes.put("T" + i, quote("T" + i, i));
        }
        when(quoteCacheService.getQuotes(any())).thenReturn(manyQuotes);

        assertThat(mostActiveService.getMostActive(1)).hasSize(5); // below min -> clamped up to 5
        assertThat(mostActiveService.getMostActive(50)).hasSize(10); // above max -> clamped down to 10
        assertThat(mostActiveService.getMostActive(7)).hasSize(7); // within range -> respected as-is
    }
}
