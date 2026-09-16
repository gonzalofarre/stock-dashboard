package com.stockdashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {
    /** Base64-encoded HMAC-SHA256 secret. Must be set via APP_JWT_SECRET env var in every real environment. */
    private String secret;
    private long accessTokenMinutes = 15;
    private long refreshTokenDays = 30;
}
