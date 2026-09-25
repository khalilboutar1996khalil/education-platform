package com.example.education_platform.auth.service.impl;

import com.example.education_platform.auth.dto.request.LoginRequest;
import com.example.education_platform.auth.dto.request.RegisterRequest;
import com.example.education_platform.auth.dto.response.LoginResponse;
import com.example.education_platform.auth.dto.response.TokenResponse;
import com.example.education_platform.auth.entity.PasswordResetToken;
import com.example.education_platform.auth.entity.RefreshToken;
import com.example.education_platform.auth.repository.PasswordResetTokenRepository;
import com.example.education_platform.auth.repository.RefreshTokenRepository;
import com.example.education_platform.auth.service.AuthService;
import com.example.education_platform.classcode.entity.ClassCode;
import com.example.education_platform.classcode.repository.ClassCodeRepository;
import com.example.education_platform.common.exception.BusinessException;
import com.example.education_platform.common.exception.ConflictException;
import com.example.education_platform.common.exception.InvalidRefreshTokenException;
import com.example.education_platform.mail.service.EmailService;
import com.example.education_platform.security.JwtProperties;
import com.example.education_platform.security.LoginRateLimiter;
import com.example.education_platform.security.TokenService;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.entity.UserStatus;
import com.example.education_platform.user.mapper.UserMapper;
import com.example.education_platform.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final java.time.Duration RESET_VALIDITY = java.time.Duration.ofHours(1);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final LoginRateLimiter rateLimiter;
    private final PasswordResetTokenRepository resetTokens;
    private final EmailService emailService;
    private final ClassCodeRepository classCodes;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String userAgent, String ipAddress) {
        String email = normalise(request.email());
        rateLimiter.checkAllowed(email);

        Optional<User> found = users.findByEmail(email)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .filter(candidate -> candidate.getStatus() != UserStatus.DISABLED);
        if (found.isEmpty()) {
            rateLimiter.recordFailure(email);
            // One message for every failure, so the response cannot be used to discover accounts
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = found.get();
        rateLimiter.recordSuccess(email);
        return new LoginResponse(issueTokens(user, userAgent, ipAddress), userMapper.toResponse(user));
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request, String userAgent, String ipAddress) {
        ClassCode classCode = classCodes
                .findByCodeIgnoreCaseAndActiveTrue(ClassCode.normalise(request.classCode()))
                .orElseThrow(() -> new BusinessException("Code de classe invalide ou expiré"));

        String email = normalise(request.email());
        if (users.existsByEmail(email)) {
            // Said plainly on purpose: whoever is registering already knows this address is theirs,
            // and the alternative is a silent failure they cannot act on.
            throw new ConflictException("Un compte existe déjà pour cette adresse email");
        }

        // The level comes from the code, never from the request, so a 2ᵉ AS code cannot open 3ᵉ AS.
        User user = users.save(new User(request.fullName().trim(), email,
                passwordEncoder.encode(request.password()), Role.STUDENT, classCode.getLevel()));

        return new LoginResponse(issueTokens(user, userAgent, ipAddress), userMapper.toResponse(user));
    }

    @Override
    @Transactional
    public TokenResponse refresh(String refreshToken, String userAgent, String ipAddress) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokens.findByTokenHash(hash(refreshToken))
                .filter(token -> token.isActive(now))
                .orElseThrow(() -> new InvalidRefreshTokenException("Unknown, expired or revoked refresh token"));

        // Rotation: the presented token dies with this call, so replaying it fails
        stored.revoke(now);
        return issueTokens(stored.getUser(), userAgent, ipAddress);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        // Logging out with an unknown token is not an error — the session is gone either way
        refreshTokens.findByTokenHash(hash(refreshToken))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        // Silent when the address is unknown: a different answer would leak who has an account
        users.findByEmail(normalise(email)).ifPresent(user -> {
            String raw = randomToken();
            resetTokens.save(new PasswordResetToken(user, hash(raw),
                    Instant.now().plus(RESET_VALIDITY)));
            emailService.sendPasswordReset(user.getEmail(), user.getFullName(), raw);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Instant now = Instant.now();
        PasswordResetToken stored = resetTokens.findByTokenHash(hash(token))
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(() -> new BusinessException("This reset link is no longer valid"));

        stored.consume(now);
        User user = stored.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Whoever was signed in with the old password is signed out by it changing
        refreshTokens.revokeAllForUser(user.getId(), now);
    }

    private TokenResponse issueTokens(User user, String userAgent, String ipAddress) {
        TokenService.IssuedToken access = tokenService.issueAccessToken(user);
        String raw = randomToken();
        Instant expiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl());
        refreshTokens.save(new RefreshToken(user, hash(raw), expiresAt, truncate(userAgent), ipAddress));
        return new TokenResponse(access.value(), access.expiresAt(), raw, expiresAt);
    }

    private static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JVM", e);
        }
    }

    private static String truncate(String userAgent) {
        if (userAgent == null || userAgent.length() <= 255) {
            return userAgent;
        }
        return userAgent.substring(0, 255);
    }
}
