package com.stockdashboard.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Verified against a real Twelve Data /quote response (single and
 * multi-symbol shapes both confirmed, including "name" and "volume" fields).
 *
 * Parses the body as a String and reads it into a (Jackson 2) JsonNode with
 * our own ObjectMapper, rather than `.retrieve().body(JsonNode.class)` —
 * Spring Boot 4's RestClient auto-configures Jackson 3 (`tools.jackson`)
 * converters by default, which can't produce this classic
 * `com.fasterxml.jackson.databind.JsonNode` (the type jjwt-jackson and the
 * rest of this codebase use); asking for it that way failed with "Type
 * definition error" against the real API. Confirmed the hard way.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "twelvedata")
public class TwelveDataClient implements MarketDataClient {

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", symbolParam)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(String.class);
            response = body != null ? objectMapper.readTree(body) : null;
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
            String name = node.hasNonNull("name") ? node.get("name").asText() : null;
            return java.util.Optional.of(new Quote(ticker, price, changePercent, volume, name, Instant.now()));
        } catch (Exception e) {
            log.warn("Could not parse Twelve Data quote node: {}", node, e);
            return java.util.Optional.empty();
        }
    }
}
