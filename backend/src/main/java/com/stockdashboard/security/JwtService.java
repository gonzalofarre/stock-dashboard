package com.stockdashboard.security;

import com.stockdashboard.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtProperties.getSecret()));
    }

    public String generateAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.getAccessTokenMinutes(), ChronoUnit.MINUTES)))
                .signWith(key())
                .compact();
    }

    /** Throws JwtException (expired/malformed/invalid signature) if the token isn't valid — callers decide how to react. */
    public Claims parseAndValidate(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    /** Opaque, high-entropy refresh token — not a JWT, so it carries no forgeable claims of its own;
     * validity is entirely determined by whether its hash exists, un-revoked and unexpired, in the DB. */
    public String generateOpaqueRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public long getAccessTokenMinutes() {
        return jwtProperties.getAccessTokenMinutes();
    }

    public long getRefreshTokenDays() {
        return jwtProperties.getRefreshTokenDays();
    }
}
