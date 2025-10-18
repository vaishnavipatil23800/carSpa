/**
 * AuthDto.java — all auth-related request/response shapes in one place.
 * Nested static classes keep things tidy without creating 5 separate files.
 */
package com.carspa.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class AuthDto {

    // ── Inbound ──

    @Getter @Setter
    public static class RegisterRequest {

        @NotBlank(message = "Full name is required")
        private String fullName;

        @Email(message = "Enter a valid email")
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        private String phone;  // optional
    }

    @Getter @Setter
    public static class LoginRequest {

        @Email
        @NotBlank(message = "Email is required")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    // ── Outbound ──

    @Getter
    @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String tokenType;
        private long   expiresIn;    // seconds
        private String email;
        private String fullName;
        private String role;

        // convenience constructor
        public TokenResponse(String accessToken, long expiresIn,
                             String email, String fullName, String role) {
            this.accessToken = accessToken;
            this.tokenType   = "Bearer";
            this.expiresIn   = expiresIn;
            this.email       = email;
            this.fullName    = fullName;
            this.role        = role;
        }
    }

    @Getter
    @Builder
    public static class UserProfile {
        private Long    id;
        private String  fullName;
        private String  email;
        private String  phone;
        private String  role;
        private boolean active;
    }
}
