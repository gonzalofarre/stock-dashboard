package com.stockdashboard.earnings;

import java.time.LocalDate;

public record UpcomingEarningsResponse(String ticker, String companyName, LocalDate reportDate, ReportTime time) {
}
