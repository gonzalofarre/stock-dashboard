package com.stockdashboard.strategy;

import java.math.BigDecimal;

public record StrategySignalResponse(
        String ticker,
        String companyName,
        SignalType type,
        Direction direction,
        int minutesAgo,
        BigDecimal targetPrice
) {
}
