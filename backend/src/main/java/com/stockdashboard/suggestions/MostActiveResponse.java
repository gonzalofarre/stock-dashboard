package com.stockdashboard.suggestions;

import java.math.BigDecimal;

public record MostActiveResponse(String ticker, String name, BigDecimal price, BigDecimal changePercent, long volume) {
}
