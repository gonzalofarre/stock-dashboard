package com.stockdashboard.security;

import com.stockdashboard.auth.AuthService;
import com.stockdashboard.auth.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * After Spring Security finishes the Google OAuth2 dance, this hands off to
 * our own JWT issuance (see AuthService.handleOAuthLogin) instead of the
 * default session-based flow, then redirects to a frontend callback route
 * carrying the tokens in the URL FRAGMENT (after #) rather than a query
 * string — fragments never get sent to the server or logged, only read by
 * the frontend's own JS.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String providerUserId = oAuth2User.getName();
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        AuthResponse tokens = authService.handleOAuthLogin(
                "google", providerUserId, email,
                firstName != null ? firstName : "", lastName != null ? lastName : ""
        );

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth-callback")
                .fragment("accessToken=" + tokens.accessToken() + "&refreshToken=" + tokens.refreshToken())
                .build()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}
