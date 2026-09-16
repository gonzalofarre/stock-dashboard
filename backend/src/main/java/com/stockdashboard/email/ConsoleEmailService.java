package com.stockdashboard.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default email "sender" for local development: logs the code instead of
 * actually emailing it, so the register -> verify flow is testable end to
 * end before SMTP credentials exist. Switch to real delivery by setting
 * app.email.mode=smtp (see application.yml) plus SMTP env vars.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.email.mode", havingValue = "console", matchIfMissing = true)
public class ConsoleEmailService implements EmailService {

    @Override
    public void sendVerificationCode(String toEmail, String firstName, String code) {
        log.info("[DEV EMAIL] Verification code for {} <{}>: {}", firstName, toEmail, code);
    }
}
