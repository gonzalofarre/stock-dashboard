package com.stockdashboard.stocks;

import java.math.BigDecimal;

public record StockQuoteResponse(String ticker, String name, BigDecimal price, BigDecimal changePercent, Long volume, boolean quoteAvailable) {
}
