package com.stockdashboard.earnings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Default earnings source for local development: fabricates, per ticker, one
 * upcoming report (no actual EPS yet) and one recent past report (with an
 * actual EPS, so a surprise % can be computed) — both deterministic per
 * ticker so the dashboard looks stable across requests. Set EARNINGS_PROVIDER
 * to "fmp" for real data once an FMP key exists.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.earnings.provider", havingValue = "mock", matchIfMissing = true)
public class MockEarningsClient implements EarningsClient {

    @Override
    public List<EarningsEvent> getEarningsCalendar(List<String> tickers, LocalDate from, LocalDate to) {
        log.info("[MOCK EARNINGS] Generating fake earnings calendar for {} tickers — set FMP_API_KEY for real data.",
                tickers.size());
        LocalDate today = LocalDate.now();
        List<EarningsEvent> events = new ArrayList<>();
        for (String ticker : tickers) {
            Random random = new Random(ticker.hashCode());

            int upcomingOffsetDays = 1 + random.nextInt(20);
            int pastOffsetDays = 1 + random.nextInt(25);
            BigDecimal epsEstimatedUpcoming = randomEps(random);
            BigDecimal epsEstimatedPast = randomEps(random);
            double surpriseFraction = (random.nextDouble() * 0.40) - 0.15; // -15% .. +25%
            ReportTime upcomingTime = randomTime(random);
            ReportTime pastTime = randomTime(random);

            addIfInRange(events, ticker, today.plusDays(upcomingOffsetDays), upcomingTime, epsEstimatedUpcoming, null,
                    from, to);

            BigDecimal epsActualPast = epsEstimatedPast
                    .multiply(BigDecimal.valueOf(1 + surpriseFraction))
                    .setScale(2, RoundingMode.HALF_UP);
            addIfInRange(events, ticker, today.minusDays(pastOffsetDays), pastTime, epsEstimatedPast, epsActualPast,
                    from, to);
        }
        return events;
    }

    private void addIfInRange(List<EarningsEvent> events, String ticker, LocalDate date, ReportTime time,
            BigDecimal epsEstimated, BigDecimal epsActual, LocalDate from, LocalDate to) {
        if (!date.isBefore(from) && !date.isAfter(to)) {
            events.add(new EarningsEvent(ticker, date, time, epsEstimated, epsActual));
        }
    }

    private BigDecimal randomEps(Random random) {
        return BigDecimal.valueOf(0.5 + random.nextDouble() * 4.5).setScale(2, RoundingMode.HALF_UP);
    }

    private ReportTime randomTime(Random random) {
        ReportTime[] values = { ReportTime.BEFORE_OPEN, ReportTime.AFTER_CLOSE };
        return values[random.nextInt(values.length)];
    }
}
