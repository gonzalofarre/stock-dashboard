package com.stockdashboard.earnings;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One company's earnings report, past or future. {@code epsActual} is null
 * until the report has actually happened — that's what tells
 * {@link EarningsService} apart an upcoming event from one it can compute a
 * surprise % for.
 */
public record EarningsEvent(
        String ticker,
        LocalDate reportDate,
        ReportTime time,
        BigDecimal epsEstimated,
        BigDecimal epsActual
) {
}
