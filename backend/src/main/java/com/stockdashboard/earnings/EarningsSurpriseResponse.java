package com.stockdashboard.earnings;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EarningsSurpriseResponse(String ticker, LocalDate reportDate, BigDecimal surprisePercent) {
}
