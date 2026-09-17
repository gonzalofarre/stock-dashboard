package com.stockdashboard.earnings;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EarningsSurpriseResponse(String ticker, String companyName, LocalDate reportDate, BigDecimal surprisePercent) {
}
