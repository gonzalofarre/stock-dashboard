package com.stockdashboard.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Verified against a real Twelve Data /time_series response — including that
 * it DOES support the same comma-separated multi-symbol batching as /quote
 * (undocumented as such, but confirmed working), so this sends ONE request
 * for however many tickers it's asked for rather than one per ticker.
 * Twelve Data still charges 1 API credit per symbol either way though —
 * confirmed too, the hard way (a single 50-symbol request came back
 * `429 "52 API credits were used, with the current limit being 8"` on the
 * free tier) — so staying within budget is PriceSeriesCacheService's job
 * (it never asks this client for more than max-stale-fetch-per-call tickers
 * at once), not something batching alone fixes.
 *
 * Reads the body as a String into a (Jackson 2) JsonNode via our own
 * ObjectMapper rather than `.retrieve().body(JsonNode.class)` — see
 * TwelveDataClient's Javadoc for why (Spring Boot 4's RestClient defaults to
 * Jackson 3 converters, which can't produce this classic JsonNode type).
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.strategy.provider", havingValue = "twelvedata")
public class TwelveDataPriceSeriesClient implements PriceSeriesClient {

    private static final String INTERVAL = "15min";
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TwelveDataPriceSeriesClient(@Value("${app.marketdata.twelvedata.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder().baseUrl("https://api.twelvedata.com").build();
    }

    @Override
    public Map<String, List<Bar>> getIntradaySeries(List<String> tickers, int barCount) {
        if (tickers.isEmpty()) {
            return Map.of();
        }
        String symbolParam = String.join(",", tickers);
        JsonNode response;
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/time_series")
                            .queryParam("symbol", symbolParam)
                            .queryParam("interval", INTERVAL)
                            .queryParam("outputsize", barCount)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(String.class);
            response = body != null ? objectMapper.readTree(body) : null;
        } catch (Exception e) {
            log.warn("Twelve Data time_series request failed for {}: {}", tickers, e.getMessage());
            return Map.of();
        }
        if (response == null) {
            return Map.of();
        }

        // A single-symbol request returns one flat {meta, values, status} object;
        // a multi-symbol request returns an object keyed by symbol, each with its
        // own {meta, values, status} — same asymmetry as /quote (see TwelveDataClient).
        Map<String, List<Bar>> result = new HashMap<>();
        if (response.has("meta")) {
            parseValues(tickers.get(0), response).ifPresent(bars -> result.put(tickers.get(0), bars));
        } else {
            Iterator<String> fieldNames = response.fieldNames();
            while (fieldNames.hasNext()) {
                String ticker = fieldNames.next();
                parseValues(ticker, response.get(ticker)).ifPresent(bars -> result.put(ticker, bars));
            }
        }
        return result;
    }

    private Optional<List<Bar>> parseValues(String ticker, JsonNode node) {
        if (node == null || !node.path("values").isArray()) {
            return Optional.empty();
        }
        try {
            List<Bar> bars = new ArrayList<>();
            for (JsonNode valueNode : node.get("values")) {
                Instant time = LocalDateTime.parse(valueNode.get("datetime").asText(), DATETIME_FORMAT)
                        .atZone(ZoneId.systemDefault()).toInstant();
                BigDecimal close = new BigDecimal(valueNode.get("close").asText());
                bars.add(new Bar(time, close));
            }
            // Twelve Data returns most-recent-first; StrategyService needs oldest-first.
            Collections.reverse(bars);
            return Optional.of(bars);
        } catch (Exception e) {
            log.warn("Could not parse Twelve Data time_series response for {}: {}", ticker, node, e);
            return Optional.empty();
        }
    }
}
