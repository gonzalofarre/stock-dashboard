package com.stockdashboard.earnings;

import java.time.LocalDate;

public record UpcomingEarningsResponse(String ticker, LocalDate reportDate, ReportTime time) {
}
