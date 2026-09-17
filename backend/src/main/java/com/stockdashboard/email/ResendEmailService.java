package com.stockdashboard.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends over Resend's HTTPS API instead of SMTP: most PaaS providers
 * (Railway included, confirmed via its own network flow logs — outbound
 * port 587 packets were silently dropped) block outbound SMTP ports to
 * prevent spam relaying, so a real JavaMailSender-based service never
 * connects in production. HTTPS on 443 has no such restriction.
 *
 * Uses the "onboarding@resend.dev" sandbox sender, which only delivers to
 * the Resend account's own verified address — fine while there's no
 * verified custom domain, but real third-party users won't receive mail
 * until one is added (see Resend's domain verification).
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.email.mode", havingValue = "resend")
public class ResendEmailService implements EmailService {

    private final RestClient restClient;

    public ResendEmailService(@Value("${app.email.resend.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public void sendVerificationCode(String toEmail, String firstName, String code) {
        String text = """
                Hola %s,

                Tu código de verificación es: %s

                Vence en 15 minutos. Si no creaste esta cuenta, ignorá este mensaje.
                """.formatted(firstName, code);

        restClient.post()
                .uri("/emails")
                .body(Map.of(
                        "from", "StockDash <onboarding@resend.dev>",
                        "to", List.of(toEmail),
                        "subject", "Tu código de verificación",
                        "text", text
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
