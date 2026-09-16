package com.stockdashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Deliberately its own tiny configuration class with no other constructor
 * dependencies. SecurityConfig depends (transitively, via
 * OAuth2LoginSuccessHandler) on AuthService, which depends on PasswordEncoder
 * — defining PasswordEncoder inside SecurityConfig itself created a circular
 * bean-creation dependency. Keep it isolated here instead of moving it back.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
