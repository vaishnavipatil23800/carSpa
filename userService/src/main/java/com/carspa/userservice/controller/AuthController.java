/**
 * AuthController.java — register, login, profile, and admin endpoints.
 *
 * User identity for protected endpoints comes from @AuthenticationPrincipal
 * which Spring Security populates after the JwtFilter validates the token.
 */
package com.carspa.userservice.controller;

import com.carspa.userservice.dto.AuthDto;
import com.carspa.userservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Register, login, profile, admin user management")
public class AuthController {

    private final AuthService authService;

    // ── PUBLIC endpoints — no token required ──

    @PostMapping("/auth/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<AuthDto.TokenResponse> register(
        @Valid @RequestBody AuthDto.RegisterRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(authService.register(request));
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Login and get a JWT token")
    public ResponseEntity<AuthDto.TokenResponse> login(
        @Valid @RequestBody AuthDto.LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── PROTECTED endpoints — valid JWT required ──

    @GetMapping("/profile")
    @Operation(summary = "Get the logged-in user's profile",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AuthDto.UserProfile> getProfile(
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(authService.getProfile(principal.getUsername()));
    }

    // ── ADMIN endpoints — ROLE_ADMIN required ──

    @GetMapping("/admin/all")
    @Operation(summary = "Admin — list all registered users",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<AuthDto.UserProfile>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PatchMapping("/admin/{userId}/deactivate")
    @Operation(summary = "Admin — deactivate a user account",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> deactivateUser(@PathVariable Long userId) {
        authService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
