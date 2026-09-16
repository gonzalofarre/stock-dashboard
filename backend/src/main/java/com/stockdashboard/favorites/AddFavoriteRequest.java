package com.stockdashboard.favorites;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddFavoriteRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z.\\-]{1,10}$", message = "Ticker looks invalid")
        String ticker
) {
}
