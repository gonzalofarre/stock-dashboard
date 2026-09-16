package com.stockdashboard.email;

public interface EmailService {
    void sendVerificationCode(String toEmail, String firstName, String code);
}
