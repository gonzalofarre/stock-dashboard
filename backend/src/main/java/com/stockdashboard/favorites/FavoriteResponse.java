package com.stockdashboard.favorites;

import java.math.BigDecimal;
import java.time.Instant;

public record FavoriteResponse(
        String ticker,
        String name,
        BigDecimal price,
        BigDecimal changePercent,
        boolean quoteAvailable,
        Instant addedAt
) {
}
