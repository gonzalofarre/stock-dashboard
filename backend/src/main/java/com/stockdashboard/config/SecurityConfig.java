package com.stockdashboard.config;

import com.stockdashboard.security.JwtAuthenticationFilter;
import com.stockdashboard.security.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    // Only present when GoogleOAuth2Config's bean actually exists (both
    // GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET set) — ObjectProvider lets us
    // ask "does it exist?" without failing to wire this whole config when it
    // doesn't.
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Explicit paths, not a "/api/auth/**" wildcard — that would have made
                        // /api/auth/me (which must require a valid access token) public too.
                        .requestMatchers(
                                "/api/auth/register", "/api/auth/verify-email", "/api/auth/resend-verification",
                                "/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/auth/config",
                                "/oauth2/**", "/login/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // Without this, Spring Security's default reaction to an
                // unauthenticated request on a REST API is a 302 redirect to
                // the OAuth2 authorization endpoint (a browser-login-flow
                // default that leaks through from .oauth2Login()) instead of
                // a plain 401 — very much not what a JSON API client expects.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                ))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Only wire Google login when GoogleOAuth2Config actually created a
        // registration (both GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET set) —
        // calling .oauth2Login() with no ClientRegistrationRepository bean in
        // the context fails at startup.
        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler));
        }

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
