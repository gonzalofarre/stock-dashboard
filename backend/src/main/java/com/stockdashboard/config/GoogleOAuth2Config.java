package com.stockdashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * "Continuar con Google" is entirely optional — this bean only exists when
 * both GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are actually set.
 *
 * This used to be a conditional YAML document
 * (spring.config.activate.on-property: GOOGLE_CLIENT_ID) directly on the
 * properties-based registration. That doesn't do what its own comment
 * claimed: on-property only skips a document when the property is the
 * literal string "false" — an ABSENT property still activates it. Confirmed
 * against a real, guaranteed-clean environment (no GOOGLE_CLIENT_ID set
 * anywhere): the old config still built a "google" ClientRegistration with
 * an unresolved "${GOOGLE_CLIENT_ID}" literal as the client id, and
 * /oauth2/authorization/google happily redirected to Google with that
 * broken value — Google's own error page, not this app, was quietly doing
 * the rejecting. @ConditionalOnProperty (used here, at the bean level)
 * doesn't share that ambiguity: it's a genuinely reliable "all of these
 * properties must be present" check, and SecurityConfig only wires
 * .oauth2Login() when this bean actually exists.
 */
@Configuration
public class GoogleOAuth2Config {

    @Bean
    @ConditionalOnProperty(name = {"GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET"})
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${GOOGLE_CLIENT_ID}") String clientId,
            @Value("${GOOGLE_CLIENT_SECRET}") String clientSecret
    ) {
        // CommonOAuth2Provider (the old shortcut for well-known providers'
        // endpoints) doesn't exist in this Spring Security version — this
        // does the equivalent by fetching Google's own OIDC discovery
        // document instead of hand-copying its endpoint URLs. Only runs at
        // startup, and only when this bean is actually being created (i.e.
        // only when both properties above are set).
        ClientRegistration google = ClientRegistrations.fromOidcIssuerLocation("https://accounts.google.com")
                .registrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope("openid", "profile", "email")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
