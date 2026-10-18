package com.portal.security;

import com.portal.entity.AppUser;
import com.portal.entity.RefreshToken;
import com.portal.exception.ApiException;
import com.portal.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateRawToken() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional
    public RefreshToken createRefreshToken(AppUser user) {
        String rawToken = generateRawToken();
        String hash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(refreshToken);
        // Temporarily store raw token on the entity field so caller can send it to client
        refreshToken.setTokenHash(rawToken);
        return refreshToken;
    }

    @Transactional
    public RefreshToken verifyAndRotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException("Refresh token is required", HttpStatus.UNAUTHORIZED);
        }

        String hash = hashToken(rawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException("Invalid or unknown refresh token", HttpStatus.UNAUTHORIZED));

        if (existing.isRevoked()) {
            // Compromised token reuse detected: revoke all tokens for this user family
            refreshTokenRepository.revokeAllUserTokens(existing.getUser());
            log.warn("Compromised refresh token reused for user: {}", existing.getUser().getEmail());
            throw new ApiException("Revoked refresh token used. All active sessions invalidated for security.", HttpStatus.UNAUTHORIZED);
        }

        if (existing.isExpired()) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw new ApiException("Refresh token expired. Please login again.", HttpStatus.UNAUTHORIZED);
        }

        // Rotate token
        String newRawToken = generateRawToken();
        String newHash = hashToken(newRawToken);

        existing.setRevoked(true);
        existing.setReplacedByTokenHash(newHash);
        refreshTokenRepository.save(existing);

        RefreshToken newToken = RefreshToken.builder()
                .user(existing.getUser())
                .tokenHash(newHash)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        refreshTokenRepository.save(newToken);
        newToken.setTokenHash(newRawToken);
        return newToken;
    }

    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        String hash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    public ResponseCookie createRefreshCookie(String rawToken, boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")
                .maxAge(refreshExpirationMs / 1000)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie clearRefreshCookie(boolean secure) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}
