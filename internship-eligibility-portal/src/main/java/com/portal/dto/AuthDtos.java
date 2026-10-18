package com.portal.dto;

import com.portal.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\\-]).{8,}$",
                message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character (@#$%^&+=!_-)"
        )
        private String password;

        @NotNull(message = "Role is required")
        private Role role; // STUDENT, FACULTY, COMPANY

        // STUDENT / FACULTY
        private String name;
        private String prn;
        private String department;

        // COMPANY
        private String companyName;
        private String industry;

        // FACULTY
        private String facultyDepartment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String token;          // backward compatible getter
        private String accessToken;    // explicit modern naming
        private String refreshToken;
        private String tokenType;
        private String email;
        private Role role;
        private Long profileId;

        public AuthResponse(String token, String email, Role role, Long profileId) {
            this.token = token;
            this.accessToken = token;
            this.tokenType = "Bearer";
            this.email = email;
            this.role = role;
            this.profileId = profileId;
        }

        public AuthResponse(String accessToken, String refreshToken, String email, Role role, Long profileId) {
            this.token = accessToken;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.tokenType = "Bearer";
            this.email = email;
            this.role = role;
            this.profileId = profileId;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
}
