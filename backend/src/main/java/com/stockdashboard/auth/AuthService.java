package com.stockdashboard.auth;

import com.stockdashboard.auth.dto.AuthResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int VERIFICATION_CODE_TTL_MINUTES = 15;

    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict("Email already registered");
        }
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .emailVerified(false)
                .build();
        user = userRepository.save(user);
        sendNewVerificationCode(user);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> ApiException.badRequest("Invalid email or code"));
        if (user.isEmailVerified()) {
            throw ApiException.badRequest("Email already verified");
        }
        EmailVerificationCode verification = verificationCodeRepository
                .findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired code"));
        if (verification.isUsed() || verification.isExpired() || !verification.getCode().equals(request.code())) {
            throw ApiException.badRequest("Invalid or expired code");
        }
        verification.setUsedAt(Instant.now());
        user.setEmailVerified(true);
    }

    @Transactional
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("No account with that email"));
        if (user.isEmailVerified()) {
            throw ApiException.badRequest("Email already verified");
        }
        sendNewVerificationCode(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }
        if (!user.isEmailVerified()) {
            throw ApiException.unauthorized("Email not verified");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = TokenHasher.sha256(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (!stored.isActive()) {
            throw ApiException.unauthorized("Refresh token expired or revoked");
        }
        // Rotate on every use: revoke the presented token and issue a brand new pair.
        // Limits how long a leaked refresh token stays useful to whoever leaked it.
        stored.setRevokedAt(Instant.now());
        return issueTokens(stored.getUser());
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserSummary getUserSummary(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        return new AuthResponse.UserSummary(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }

    @Transactional
    public void logout(RefreshRequest request) {
        String hash = TokenHasher.sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> t.setRevokedAt(Instant.now()));
    }

    /**
     * Find-or-create for a Google login. Google has already verified the
     * email, so a brand-new account is marked verified immediately — no
     * verification code round-trip needed for OAuth users. If a local
     * (password-based) account already exists with this email, the Google
     * identity is linked to it rather than creating a duplicate user.
     */
    @Transactional
    public AuthResponse handleOAuthLogin(String provider, String providerUserId, String email, String firstName, String lastName) {
        User user = oAuthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(OAuthAccount::getUser)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> userRepository.save(User.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .emailVerified(true)
                        .build()));

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
        }

        boolean alreadyLinked = oAuthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId).isPresent();
        if (!alreadyLinked) {
            oAuthAccountRepository.save(OAuthAccount.builder()
                    .user(user)
                    .provider(provider)
                    .providerUserId(providerUserId)
                    .build());
        }

        return issueTokens(user);
    }

    private void sendNewVerificationCode(User user) {
        String code = generateSixDigitCode();
        EmailVerificationCode verification = EmailVerificationCode.builder()
                .user(user)
                .code(code)
                .expiresAt(Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES))
                .build();
        verificationCodeRepository.save(verification);
        emailService.sendVerificationCode(user.getEmail(), user.getFirstName(), code);
    }

    private String generateSixDigitCode() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = jwtService.generateOpaqueRefreshToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.sha256(rawRefreshToken))
                .expiresAt(Instant.now().plus(jwtService.getRefreshTokenDays(), ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                jwtService.getAccessTokenMinutes() * 60,
                new AuthResponse.UserSummary(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail())
        );
    }
}
