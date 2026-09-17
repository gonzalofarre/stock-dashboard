package com.stockdashboard.stocks;

import java.math.BigDecimal;

public record StockQuoteResponse(String ticker, BigDecimal price, BigDecimal changePercent, Long volume, boolean quoteAvailable) {
}
