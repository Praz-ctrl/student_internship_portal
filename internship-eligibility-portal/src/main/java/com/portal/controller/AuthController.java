package com.portal.controller;

import com.portal.dto.AuthDtos.AuthResponse;
import com.portal.dto.AuthDtos.LoginRequest;
import com.portal.dto.AuthDtos.RefreshTokenRequest;
import com.portal.dto.AuthDtos.RegisterRequest;
import com.portal.security.RefreshTokenService;
import com.portal.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest,
                                                 HttpServletResponse httpResponse) {
        AuthResponse response = authService.register(request, getClientIp(httpRequest));
        attachRefreshCookie(httpResponse, response.getRefreshToken(), httpRequest.isSecure());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        AuthResponse response = authService.login(request, getClientIp(httpRequest));
        attachRefreshCookie(httpResponse, response.getRefreshToken(), httpRequest.isSecure());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = RefreshTokenService.REFRESH_COOKIE_NAME, required = false) String cookieRefreshToken,
                                                @RequestBody(required = false) RefreshTokenRequest requestBody,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        String tokenToVerify = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (requestBody != null ? requestBody.getRefreshToken() : null);

        AuthResponse response = authService.refreshToken(tokenToVerify);
        attachRefreshCookie(httpResponse, response.getRefreshToken(), httpRequest.isSecure());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@CookieValue(name = RefreshTokenService.REFRESH_COOKIE_NAME, required = false) String cookieRefreshToken,
                                                      @RequestBody(required = false) RefreshTokenRequest requestBody,
                                                      HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {
        String tokenToRevoke = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (requestBody != null ? requestBody.getRefreshToken() : null);

        authService.logout(tokenToRevoke);
        ResponseCookie clearCookie = refreshTokenService.clearRefreshCookie(httpRequest.isSecure());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private void attachRefreshCookie(HttpServletResponse response, String refreshToken, boolean secure) {
        if (refreshToken != null) {
            ResponseCookie cookie = refreshTokenService.createRefreshCookie(refreshToken, secure);
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
    }
}
