package com.stockdashboard.email;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.email.mode", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationCode(String toEmail, String firstName, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Tu código de verificación");
        message.setText("""
                Hola %s,

                Tu código de verificación es: %s

                Vence en 15 minutos. Si no creaste esta cuenta, ignorá este mensaje.
                """.formatted(firstName, code));
        mailSender.send(message);
    }
}
