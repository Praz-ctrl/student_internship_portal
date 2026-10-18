package com.portal.security;

import com.portal.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimiterTest {

    @Test
    void rateLimiterAllowsUnderLimitAndBlocksOverLimit() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String ip = "192.168.1.100";

        // Up to 10 requests should be allowed
        for (int i = 0; i < 10; i++) {
            assertDoesNotThrow(() -> limiter.checkRateLimit(ip));
        }

        // 11th request in the same window should throw ApiException
        assertThrows(ApiException.class, () -> limiter.checkRateLimit(ip));
    }
}
