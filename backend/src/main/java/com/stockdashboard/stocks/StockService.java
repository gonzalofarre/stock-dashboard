package com.stockdashboard.stocks;

import com.stockdashboard.common.ApiException;
import com.stockdashboard.marketdata.Quote;
import com.stockdashboard.marketdata.QuoteCacheService;
import com.stockdashboard.suggestions.StockUniverse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StockService {

    // Same shape as AddFavoriteRequest's ticker pattern — kept in sync deliberately.
    private static final Pattern TICKER_PATTERN = Pattern.compile("^[A-Za-z.\\-]{1,10}$");

    private final QuoteCacheService quoteCacheService;

    public StockQuoteResponse getQuote(String rawTicker) {
        if (rawTicker == null || !TICKER_PATTERN.matcher(rawTicker).matches()) {
            throw ApiException.badRequest("Ticker looks invalid");
        }
        String ticker = rawTicker.toUpperCase();

        Map<String, Quote> quotes = quoteCacheService.getQuotes(List.of(ticker));
        Quote quote = quotes.get(ticker);
        boolean available = quote != null;
        String name = available && quote.name() != null ? quote.name() : StockUniverse.COMPANY_NAMES.get(ticker);

        return new StockQuoteResponse(
                ticker,
                name,
                available ? quote.price() : null,
                available ? quote.changePercent() : null,
                available ? quote.volume() : null,
                available
        );
    }
}
