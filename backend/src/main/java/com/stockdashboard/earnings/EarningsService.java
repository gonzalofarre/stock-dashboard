package com.stockdashboard.earnings;

import com.stockdashboard.suggestions.StockUniverse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EarningsService {

    private static final int MIN_RESULTS = 5;
    private static final int MAX_RESULTS = 10;
    private static final int UPCOMING_WINDOW_DAYS = 21;
    private static final int SURPRISE_WINDOW_DAYS = 30;

    private final EarningsClient earningsClient;

    public List<UpcomingEarningsResponse> getUpcoming(int requestedLimit) {
        int limit = Math.clamp(requestedLimit, MIN_RESULTS, MAX_RESULTS);
        LocalDate today = LocalDate.now();

        List<EarningsEvent> events =
                earningsClient.getEarningsCalendar(StockUniverse.LIQUID_US_STOCKS, today, today.plusDays(UPCOMING_WINDOW_DAYS));

        return events.stream()
                .filter(e -> !e.reportDate().isBefore(today))
                .sorted(Comparator.comparing(EarningsEvent::reportDate).thenComparing(EarningsEvent::ticker))
                .limit(limit)
                .map(e -> new UpcomingEarningsResponse(e.ticker(), e.reportDate(), e.time()))
                .toList();
    }

    public List<EarningsSurpriseResponse> getBestSurprises(int requestedLimit) {
        int limit = Math.clamp(requestedLimit, MIN_RESULTS, MAX_RESULTS);
        LocalDate today = LocalDate.now();

        List<EarningsEvent> events = earningsClient.getEarningsCalendar(
                StockUniverse.LIQUID_US_STOCKS, today.minusDays(SURPRISE_WINDOW_DAYS), today.minusDays(1));

        return events.stream()
                .filter(e -> e.epsActual() != null && e.epsEstimated() != null
                        && e.epsEstimated().compareTo(BigDecimal.ZERO) != 0)
                .map(e -> new EarningsSurpriseResponse(e.ticker(), e.reportDate(), surprisePercent(e)))
                .sorted(Comparator.comparing(EarningsSurpriseResponse::surprisePercent).reversed())
                .limit(limit)
                .toList();
    }

    private BigDecimal surprisePercent(EarningsEvent e) {
        return e.epsActual().subtract(e.epsEstimated())
                .divide(e.epsEstimated().abs(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
