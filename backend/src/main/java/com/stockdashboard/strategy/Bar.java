package com.stockdashboard.strategy;

import java.math.BigDecimal;
import java.time.Instant;

/** One intraday price bar — only the close is needed for EMA/MACD crossovers. */
public record Bar(Instant time, BigDecimal close) {
}
