package com.portal.security;

import com.portal.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS_PER_WINDOW = 10;
    private static final long WINDOW_SECONDS = 60;

    private static class Window {
        long windowStartEpochSecond;
        int count;

        Window(long nowEpochSecond) {
            this.windowStartEpochSecond = nowEpochSecond;
            this.count = 1;
        }
    }

    private final Map<String, Window> ipAttempts = new ConcurrentHashMap<>();

    public void checkRateLimit(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }

        long nowSec = Instant.now().getEpochSecond();
        ipAttempts.compute(clientIp, (key, current) -> {
            if (current == null || (nowSec - current.windowStartEpochSecond) >= WINDOW_SECONDS) {
                return new Window(nowSec);
            }
            if (current.count >= MAX_ATTEMPTS_PER_WINDOW) {
                long remainingSec = WINDOW_SECONDS - (nowSec - current.windowStartEpochSecond);
                throw new ApiException("Too many requests from this IP. Please wait " + remainingSec + " seconds before trying again.", HttpStatus.TOO_MANY_REQUESTS);
            }
            current.count++;
            return current;
        });
    }
}
