package com.stockdashboard.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * NOTE: written against Twelve Data's documented /quote endpoint, but never
 * exercised against a real API key or live response — there was none
 * available while building this. Verify against a real key before trusting
 * it; the single-vs-multiple-symbol response shape difference handled below
 * (see getQuotes) is the detail most likely to need adjustment if their API
 * has since changed.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "twelvedata")
public class TwelveDataClient implements MarketDataClient {

    private final RestClient restClient;
    private final String apiKey;

    public TwelveDataClient(@Value("${app.marketdata.twelvedata.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder().baseUrl("https://api.twelvedata.com").build();
    }

    @Override
    public Map<String, Quote> getQuotes(List<String> tickers) {
        if (tickers.isEmpty()) {
            return Map.of();
        }
        String symbolParam = String.join(",", tickers);
        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", symbolParam)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.warn("Twelve Data request failed for {}: {}", tickers, e.getMessage());
            return Map.of();
        }
        if (response == null) {
            return Map.of();
        }

        // A single-symbol request returns one flat quote object; a multi-symbol
        // request returns an object keyed by symbol. Detect which shape we got
        // by checking for a top-level "symbol" field.
        Map<String, Quote> quotes = new HashMap<>();
        if (response.has("symbol")) {
            parseOne(response).ifPresent(q -> quotes.put(q.ticker(), q));
        } else {
            Iterator<String> fieldNames = response.fieldNames();
            while (fieldNames.hasNext()) {
                String ticker = fieldNames.next();
                JsonNode node = response.get(ticker);
                parseOne(node).ifPresent(q -> quotes.put(q.ticker(), q));
            }
        }
        return quotes;
    }

    private java.util.Optional<Quote> parseOne(JsonNode node) {
        try {
            String ticker = node.get("symbol").asText();
            BigDecimal price = new BigDecimal(node.get("close").asText());
            BigDecimal changePercent = new BigDecimal(node.get("percent_change").asText());
            Long volume = node.hasNonNull("volume") ? node.get("volume").asLong() : null;
            return java.util.Optional.of(new Quote(ticker, price, changePercent, volume, Instant.now()));
        } catch (Exception e) {
            log.warn("Could not parse Twelve Data quote node: {}", node, e);
            return java.util.Optional.empty();
        }
    }
}
