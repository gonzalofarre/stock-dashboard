package com.stockdashboard.auth;

import com.stockdashboard.auth.dto.LoginRequest;
import com.stockdashboard.auth.dto.RefreshRequest;
import com.stockdashboard.auth.dto.RegisterRequest;
import com.stockdashboard.auth.dto.VerifyEmailRequest;
import com.stockdashboard.common.ApiException;
import com.stockdashboard.email.EmailService;
import com.stockdashboard.security.JwtService;
import com.stockdashboard.security.TokenHasher;
import com.stockdashboard.user.OAuthAccount;
import com.stockdashboard.user.OAuthAccountRepository;
import com.stockdashboard.user.User;
import com.stockdashboard.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OAuthAccountRepository oAuthAccountRepository;
    @Mock
    private EmailVerificationCodeRepository verificationCodeRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;
    @Captor
    private ArgumentCaptor<User> userCaptor;
    @Captor
    private ArgumentCaptor<EmailVerificationCode> codeCaptor;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, oAuthAccountRepository, verificationCodeRepository,
                refreshTokenRepository, passwordEncoder, jwtService, emailService
        );
        // save(...) on these mocked repositories just needs to echo back what was passed in,
        // the way a real repository would after assigning an id — most tests below rely on that.
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(1L);
            return u;
        });
        lenient().when(verificationCodeRepository.save(any(EmailVerificationCode.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private User verifiedUser(String rawPassword) {
        return User.builder()
                .id(1L).firstName("Ada").lastName("Lovelace").email("ada@example.com")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .emailVerified(true)
                .build();
    }

    // --- register ---

    @Test
    void register_rejectsADuplicateEmail() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ada", "Lovelace", "ada@example.com", "password123")
        )).isInstanceOf(ApiException.class).hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_hashesThePasswordAndSendsAVerificationCode() {
        when(userRepository.existsByEmail(any())).thenReturn(false);

        authService.register(new RegisterRequest("Ada", "Lovelace", "ada@example.com", "password123"));

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPasswordHash())).isTrue();
        assertThat(saved.isEmailVerified()).isFalse();

        verify(verificationCodeRepository).save(codeCaptor.capture());
        assertThat(codeCaptor.getValue().getCode()).matches("\\d{6}");
        verify(emailService).sendVerificationCode(eq("ada@example.com"), eq("Ada"), anyString());
    }

    // --- verifyEmail ---

    @Test
    void verifyEmail_acceptsTheCorrectUnexpiredCode() {
        User user = verifiedUser("password123");
        user.setEmailVerified(false);
        EmailVerificationCode code = EmailVerificationCode.builder()
                .user(user).code("123456").expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES)).build();
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(code));

        authService.verifyEmail(new VerifyEmailRequest("ada@example.com", "123456"));

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(code.isUsed()).isTrue();
    }

    @Test
    void verifyEmail_rejectsAWrongCode() {
        User user = verifiedUser("password123");
        user.setEmailVerified(false);
        EmailVerificationCode code = EmailVerificationCode.builder()
                .user(user).code("123456").expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES)).build();
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("ada@example.com", "000000")))
                .isInstanceOf(ApiException.class);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void verifyEmail_rejectsAnExpiredCode() {
        User user = verifiedUser("password123");
        user.setEmailVerified(false);
        EmailVerificationCode code = EmailVerificationCode.builder()
                .user(user).code("123456").expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)).build();
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("ada@example.com", "123456")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void verifyEmail_rejectsACodeThatWasAlreadyUsed() {
        User user = verifiedUser("password123");
        user.setEmailVerified(false);
        EmailVerificationCode code = EmailVerificationCode.builder()
                .user(user).code("123456")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .usedAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(verificationCodeRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("ada@example.com", "123456")))
                .isInstanceOf(ApiException.class);
    }

    // --- login ---

    @Test
    void login_succeedsWithCorrectCredentialsAndIssuesTokens() {
        User user = verifiedUser("password123");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(1L, "ada@example.com")).thenReturn("access-token");
        when(jwtService.generateOpaqueRefreshToken()).thenReturn("raw-refresh-token");
        when(jwtService.getRefreshTokenDays()).thenReturn(30L);
        when(jwtService.getAccessTokenMinutes()).thenReturn(15L);

        var response = authService.login(new LoginRequest("ada@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(response.expiresInSeconds()).isEqualTo(900);
        verify(refreshTokenRepository).save(argThat(rt ->
                rt.getTokenHash().equals(TokenHasher.sha256("raw-refresh-token"))
        ));
    }

    @Test
    void login_rejectsAWrongPassword() {
        User user = verifiedUser("password123");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "wrong")))
                .isInstanceOf(ApiException.class).hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_rejectsAnUnverifiedEmail() {
        User user = verifiedUser("password123");
        user.setEmailVerified(false);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "password123")))
                .isInstanceOf(ApiException.class).hasMessageContaining("not verified");
    }

    @Test
    void login_rejectsAGoogleOnlyAccountThatHasNoPassword() {
        User user = User.builder().id(1L).firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").passwordHash(null).emailVerified(true).build();
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "anything")))
                .isInstanceOf(ApiException.class).hasMessageContaining("Invalid credentials");
    }

    // --- refresh ---

    @Test
    void refresh_rotatesAnActiveTokenAndRevokesTheOldOne() {
        User user = verifiedUser("password123");
        RefreshToken stored = RefreshToken.builder()
                .id(9L).user(user).tokenHash(TokenHasher.sha256("raw-token"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS)).build();
        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256("raw-token"))).thenReturn(Optional.of(stored));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("new-access");
        when(jwtService.generateOpaqueRefreshToken()).thenReturn("new-refresh");
        when(jwtService.getRefreshTokenDays()).thenReturn(30L);
        when(jwtService.getAccessTokenMinutes()).thenReturn(15L);

        var response = authService.refresh(new RefreshRequest("raw-token"));

        assertThat(stored.getRevokedAt()).isNotNull();
        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refresh_rejectsAnAlreadyRevokedToken() {
        User user = verifiedUser("password123");
        RefreshToken stored = RefreshToken.builder()
                .id(9L).user(user).tokenHash(TokenHasher.sha256("raw-token"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .revokedAt(Instant.now().minus(1, ChronoUnit.MINUTES)).build();
        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256("raw-token"))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("raw-token")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void refresh_rejectsAnExpiredToken() {
        User user = verifiedUser("password123");
        RefreshToken stored = RefreshToken.builder()
                .id(9L).user(user).tokenHash(TokenHasher.sha256("raw-token"))
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)).build();
        when(refreshTokenRepository.findByTokenHash(TokenHasher.sha256("raw-token"))).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("raw-token")))
                .isInstanceOf(ApiException.class);
    }

    // --- OAuth login ---

    @Test
    void handleOAuthLogin_createsAVerifiedNewUserWhenNoneExists() {
        when(oAuthAccountRepository.findByProviderAndProviderUserId("google", "g-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access");
        when(jwtService.generateOpaqueRefreshToken()).thenReturn("refresh");
        when(jwtService.getRefreshTokenDays()).thenReturn(30L);
        when(jwtService.getAccessTokenMinutes()).thenReturn(15L);

        authService.handleOAuthLogin("google", "g-123", "ada@example.com", "Ada", "Lovelace");

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isEmailVerified()).isTrue();
        verify(oAuthAccountRepository).save(any(OAuthAccount.class));
    }

    @Test
    void handleOAuthLogin_linksToAnExistingLocalAccountWithTheSameEmailInsteadOfDuplicating() {
        User existing = User.builder().id(5L).firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").passwordHash("some-hash").emailVerified(true).build();
        when(oAuthAccountRepository.findByProviderAndProviderUserId("google", "g-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access");
        when(jwtService.generateOpaqueRefreshToken()).thenReturn("refresh");
        when(jwtService.getRefreshTokenDays()).thenReturn(30L);
        when(jwtService.getAccessTokenMinutes()).thenReturn(15L);

        authService.handleOAuthLogin("google", "g-123", "ada@example.com", "Ada", "Lovelace");

        verify(userRepository, never()).save(any()); // no NEW user created
        verify(oAuthAccountRepository).save(argThat(acc -> acc.getUser().getId().equals(5L)));
    }
}
