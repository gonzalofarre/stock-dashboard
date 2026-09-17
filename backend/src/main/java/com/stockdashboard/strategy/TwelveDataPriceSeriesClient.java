package com.stockdashboard.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NOTE: written against Twelve Data's documented /time_series endpoint, but
 * never exercised against a real API key or live response — same caveat as
 * TwelveDataClient. Also unlike /quote, this endpoint has no confirmed
 * multi-symbol batching in the free-tier docs, so this calls it ONCE PER
 * TICKER: scanning the full ~50-ticker universe means ~50 calls per request,
 * which will blow through a free-tier per-minute rate limit fast. Worth
 * revisiting (a smaller universe, request throttling/caching) once this is
 * actually tested against a real key.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.strategy.provider", havingValue = "twelvedata")
public class TwelveDataPriceSeriesClient implements PriceSeriesClient {

    private static final String INTERVAL = "15min";
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;
    private final String apiKey;

    public TwelveDataPriceSeriesClient(@Value("${app.marketdata.twelvedata.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder().baseUrl("https://api.twelvedata.com").build();
    }

    @Override
    public Map<String, List<Bar>> getIntradaySeries(List<String> tickers, int barCount) {
        Map<String, List<Bar>> result = new HashMap<>();
        for (String ticker : tickers) {
            fetchOne(ticker, barCount).ifPresent(bars -> result.put(ticker, bars));
        }
        return result;
    }

    private Optional<List<Bar>> fetchOne(String ticker, int barCount) {
        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/time_series")
                            .queryParam("symbol", ticker)
                            .queryParam("interval", INTERVAL)
                            .queryParam("outputsize", barCount)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.warn("Twelve Data time_series request failed for {}: {}", ticker, e.getMessage());
            return Optional.empty();
        }
        if (response == null || !response.path("values").isArray()) {
            return Optional.empty();
        }
        try {
            List<Bar> bars = new ArrayList<>();
            for (JsonNode node : response.get("values")) {
                Instant time = LocalDateTime.parse(node.get("datetime").asText(), DATETIME_FORMAT)
                        .atZone(ZoneId.systemDefault()).toInstant();
                BigDecimal close = new BigDecimal(node.get("close").asText());
                bars.add(new Bar(time, close));
            }
            // Twelve Data returns most-recent-first; StrategyService needs oldest-first.
            Collections.reverse(bars);
            return Optional.of(bars);
        } catch (Exception e) {
            log.warn("Could not parse Twelve Data time_series response for {}: {}", ticker, response, e);
            return Optional.empty();
        }
    }
}
