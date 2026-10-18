package com.portal.security;

import com.portal.entity.AppUser;
import com.portal.entity.RefreshToken;
import com.portal.entity.Role;
import com.portal.exception.ApiException;
import com.portal.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private AppUser sampleUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);
        sampleUser = AppUser.builder()
                .id(1L)
                .email("student@test.com")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    void createRefreshToken_createsAndSavesHashedToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(sampleUser);

        assertNotNull(token);
        assertNotNull(token.getTokenHash());
        assertEquals(sampleUser, token.getUser());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void verifyAndRotate_rotatesTokenSuccessfully() {
        String rawOldToken = "sample-raw-token-for-test-rotation-12345";
        String oldHash = RefreshTokenService.hashToken(rawOldToken);

        RefreshToken existing = RefreshToken.builder()
                .id(10L)
                .user(sampleUser)
                .tokenHash(oldHash)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken rotated = refreshTokenService.verifyAndRotate(rawOldToken);

        assertNotNull(rotated);
        assertTrue(existing.isRevoked());
        assertNotNull(existing.getReplacedByTokenHash());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void verifyAndRotate_throwsWhenTokenIsRevoked() {
        String rawOldToken = "compromised-token";
        String oldHash = RefreshTokenService.hashToken(rawOldToken);

        RefreshToken revoked = RefreshToken.builder()
                .id(10L)
                .user(sampleUser)
                .tokenHash(oldHash)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(revoked));

        assertThrows(ApiException.class, () -> refreshTokenService.verifyAndRotate(rawOldToken));
        verify(refreshTokenRepository, times(1)).revokeAllUserTokens(sampleUser);
    }
}
