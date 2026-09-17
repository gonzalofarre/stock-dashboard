package com.stockdashboard.suggestions;

import com.stockdashboard.marketdata.Quote;
import com.stockdashboard.marketdata.QuoteCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MostActiveService {

    private static final int MIN_RESULTS = 5;
    private static final int MAX_RESULTS = 10;

    private final QuoteCacheService quoteCacheService;

    public List<MostActiveResponse> getMostActive(int requestedLimit) {
        int limit = Math.clamp(requestedLimit, MIN_RESULTS, MAX_RESULTS);

        var quotes = quoteCacheService.getQuotes(StockUniverse.LIQUID_US_STOCKS);

        return quotes.values().stream()
                .filter(quote -> quote.volume() != null)
                .sorted(Comparator.comparingLong(Quote::volume).reversed())
                .limit(limit)
                .map(q -> new MostActiveResponse(
                        q.ticker(),
                        q.name() != null ? q.name() : StockUniverse.COMPANY_NAMES.get(q.ticker()),
                        q.price(),
                        q.changePercent(),
                        q.volume()))
                .toList();
    }
}
