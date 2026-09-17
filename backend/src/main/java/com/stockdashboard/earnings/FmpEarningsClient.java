package com.stockdashboard.earnings;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * NOTE: written against Financial Modeling Prep's documented
 * /api/v3/earning_calendar endpoint, but never exercised against a real API
 * key or live response — there was none available while building this.
 * Verify against a real key before trusting it. Unlike Twelve Data's /quote,
 * this endpoint is market-wide (not filterable by symbol server-side), so
 * this client fetches the whole date range once and filters to our universe
 * client-side.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.earnings.provider", havingValue = "fmp")
public class FmpEarningsClient implements EarningsClient {

    private final RestClient restClient;
    private final String apiKey;

    public FmpEarningsClient(@Value("${app.earnings.fmp.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder().baseUrl("https://financialmodelingprep.com").build();
    }

    @Override
    public List<EarningsEvent> getEarningsCalendar(List<String> tickers, LocalDate from, LocalDate to) {
        Set<String> wanted = Set.copyOf(tickers);
        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v3/earning_calendar")
                            .queryParam("from", from.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .queryParam("to", to.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.warn("FMP earnings calendar request failed for [{}, {}]: {}", from, to, e.getMessage());
            return List.of();
        }
        if (response == null || !response.isArray()) {
            return List.of();
        }

        List<EarningsEvent> events = new ArrayList<>();
        for (JsonNode node : response) {
            String ticker = node.path("symbol").asText(null);
            if (ticker == null || !wanted.contains(ticker)) {
                continue;
            }
            parseOne(ticker, node).ifPresent(events::add);
        }
        return events;
    }

    private java.util.Optional<EarningsEvent> parseOne(String ticker, JsonNode node) {
        try {
            LocalDate reportDate = LocalDate.parse(node.get("date").asText());
            ReportTime time = parseTime(node.path("time").asText(""));
            BigDecimal epsEstimated = node.hasNonNull("epsEstimated") ? new BigDecimal(node.get("epsEstimated").asText()) : null;
            BigDecimal epsActual = node.hasNonNull("eps") ? new BigDecimal(node.get("eps").asText()) : null;
            return java.util.Optional.of(new EarningsEvent(ticker, reportDate, time, epsEstimated, epsActual));
        } catch (Exception e) {
            log.warn("Could not parse FMP earnings node for {}: {}", ticker, node, e);
            return java.util.Optional.empty();
        }
    }

    private ReportTime parseTime(String raw) {
        return switch (raw.toLowerCase()) {
            case "bmo" -> ReportTime.BEFORE_OPEN;
            case "amc" -> ReportTime.AFTER_CLOSE;
            default -> ReportTime.UNSPECIFIED;
        };
    }
}
