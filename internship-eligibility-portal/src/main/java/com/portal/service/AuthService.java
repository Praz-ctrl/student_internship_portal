package com.portal.service;

import com.portal.dto.AuthDtos.AuthResponse;
import com.portal.dto.AuthDtos.LoginRequest;
import com.portal.dto.AuthDtos.RegisterRequest;
import com.portal.entity.*;
import com.portal.exception.ApiException;
import com.portal.repository.AppUserRepository;
import com.portal.repository.CompanyRepository;
import com.portal.repository.FacultyRepository;
import com.portal.repository.StudentRepository;
import com.portal.security.JwtUtil;
import com.portal.security.LoginRateLimiter;
import com.portal.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final FacultyRepository facultyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthResponse register(RegisterRequest req, String clientIp) {
        loginRateLimiter.checkRateLimit(clientIp);

        if (req.getRole() == Role.ADMIN) {
            throw new ApiException("Admin accounts cannot self-register", HttpStatus.FORBIDDEN);
        }
        if (appUserRepository.existsByEmail(req.getEmail())) {
            throw new ApiException("An account with this email already exists", HttpStatus.CONFLICT);
        }
        if (req.getRole() == Role.COMPANY && isBlank(req.getCompanyName())) {
            throw new ApiException("Company name is required", HttpStatus.BAD_REQUEST);
        }
        if ((req.getRole() == Role.STUDENT || req.getRole() == Role.FACULTY) && isBlank(req.getName())) {
            throw new ApiException("Name is required", HttpStatus.BAD_REQUEST);
        }

        AppUser user = appUserRepository.save(AppUser.builder()
                .email(req.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .enabled(true)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .build());

        Long profileId;
        switch (req.getRole()) {
            case STUDENT -> profileId = studentRepository.save(Student.builder()
                    .appUser(user)
                    .name(req.getName())
                    .prn(isBlank(req.getPrn()) ? null : req.getPrn().trim())
                    .department(req.getDepartment() == null ? null : req.getDepartment().trim().toUpperCase())
                    .build()).getId();
            case COMPANY -> profileId = companyRepository.save(Company.builder()
                    .appUser(user)
                    .companyName(req.getCompanyName())
                    .industry(req.getIndustry())
                    .build()).getId();
            case FACULTY -> profileId = facultyRepository.save(Faculty.builder()
                    .appUser(user)
                    .name(req.getName())
                    .department(req.getFacultyDepartment())
                    .build()).getId();
            default -> throw new ApiException("Unsupported role", HttpStatus.BAD_REQUEST);
        }

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getTokenHash(), user.getEmail(), user.getRole(), profileId);
    }

    public AuthResponse login(LoginRequest req, String clientIp) {
        loginRateLimiter.checkRateLimit(clientIp);

        String email = req.getEmail().trim().toLowerCase();
        AppUser user = appUserRepository.findByEmail(email).orElse(null);

        // Check account lockout
        if (user != null && user.getLockoutExpiry() != null) {
            if (user.getLockoutExpiry().isAfter(LocalDateTime.now())) {
                log.warn("Blocked login attempt for locked account: {}", email);
                throw new ApiException("Account is temporarily locked due to multiple failed login attempts. Please try again later.", HttpStatus.LOCKED);
            } else {
                // Lockout period has elapsed, clear lockout
                user.setLockoutExpiry(null);
                user.setFailedLoginAttempts(0);
                appUserRepository.save(user);
            }
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.getPassword()));
        } catch (BadCredentialsException e) {
            if (user != null) {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= 5) {
                    user.setLockoutExpiry(LocalDateTime.now().plusMinutes(15));
                    appUserRepository.save(user);
                    log.warn("Account locked after 5 failed login attempts: {}", email);
                    throw new ApiException("Account locked for 15 minutes due to 5 consecutive failed attempts.", HttpStatus.LOCKED);
                }
                appUserRepository.save(user);
            }
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        if (user == null) {
            throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        // Reset failed login attempts on successful login
        if (user.getFailedLoginAttempts() > 0 || user.getLockoutExpiry() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockoutExpiry(null);
            appUserRepository.save(user);
        }

        Long profileId = switch (user.getRole()) {
            case STUDENT -> studentRepository.findByAppUser(user).map(Student::getId).orElse(null);
            case COMPANY -> companyRepository.findByAppUser(user).map(Company::getId).orElse(null);
            case FACULTY -> facultyRepository.findByAppUser(user).map(Faculty::getId).orElse(null);
            case ADMIN -> null;
        };

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken.getTokenHash(), user.getEmail(), user.getRole(), profileId);
    }

    public AuthResponse refreshToken(String rawRefreshToken) {
        RefreshToken newRefreshToken = refreshTokenService.verifyAndRotate(rawRefreshToken);
        AppUser user = newRefreshToken.getUser();

        Long profileId = switch (user.getRole()) {
            case STUDENT -> studentRepository.findByAppUser(user).map(Student::getId).orElse(null);
            case COMPANY -> companyRepository.findByAppUser(user).map(Company::getId).orElse(null);
            case FACULTY -> facultyRepository.findByAppUser(user).map(Faculty::getId).orElse(null);
            case ADMIN -> null;
        };

        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return new AuthResponse(newAccessToken, newRefreshToken.getTokenHash(), user.getEmail(), user.getRole(), profileId);
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeToken(rawRefreshToken);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
