package com.stockdashboard.earnings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Uses FMP's current /stable/earnings-calendar endpoint. (An earlier version
 * of this class targeted the documented-but-actually-retired
 * /api/v3/earning_calendar — confirmed dead against a real key: FMP shut
 * down every /api/v3 endpoint on 2025-08-31 in favor of /stable. The
 * /stable response also doesn't carry a before/after-market "time" field at
 * all, unlike what the old v3 docs described, so every event here comes back
 * as ReportTime.UNSPECIFIED — see parseTime.)
 * Unlike Twelve Data's /quote, this endpoint is market-wide (not filterable
 * by symbol server-side), so this client fetches the whole date range once
 * and filters to our universe client-side.
 *
 * Reads the body as a String into a (Jackson 2) JsonNode via our own
 * ObjectMapper rather than `.retrieve().body(JsonNode.class)` — see
 * TwelveDataClient's Javadoc for why (Spring Boot 4's RestClient defaults to
 * Jackson 3 converters, which can't produce this classic JsonNode type).
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.earnings.provider", havingValue = "fmp")
public class FmpEarningsClient implements EarningsClient {

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FmpEarningsClient(@Value("${app.earnings.fmp.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder().baseUrl("https://financialmodelingprep.com").build();
    }

    @Override
    public List<EarningsEvent> getEarningsCalendar(List<String> tickers, LocalDate from, LocalDate to) {
        Set<String> wanted = Set.copyOf(tickers);
        JsonNode response;
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stable/earnings-calendar")
                            .queryParam("from", from.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .queryParam("to", to.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(String.class);
            response = body != null ? objectMapper.readTree(body) : null;
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
            BigDecimal epsActual = node.hasNonNull("epsActual") ? new BigDecimal(node.get("epsActual").asText()) : null;
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
