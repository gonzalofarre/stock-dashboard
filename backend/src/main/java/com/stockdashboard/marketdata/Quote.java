package com.stockdashboard.marketdata;

import java.math.BigDecimal;
import java.time.Instant;

public record Quote(String ticker, BigDecimal price, BigDecimal changePercent, Long volume, Instant asOf) {
}
