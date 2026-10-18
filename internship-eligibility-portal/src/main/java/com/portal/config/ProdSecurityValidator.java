package com.portal.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class ProdSecurityValidator {

    private final Environment environment;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    public ProdSecurityValidator(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateProdSecurity() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProd) {
            if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.contains("change-this") || jwtSecret.length() < 32) {
                log.error("CRITICAL SECURITY ERROR: In 'prod' profile, a secure, non-default JWT_SECRET with at least 32 characters must be provided via environment variable.");
                throw new IllegalStateException("Production startup aborted: Insecure or missing JWT_SECRET environment variable.");
            }
            log.info("Production security checks passed successfully.");
        }
    }
}
