/**
 * AuthServiceTest.java — unit tests for registration and login.
 * All dependencies are mocked — no database or Eureka needed to run these.
 */
package com.carspa.userservice.service;

import com.carspa.userservice.dto.AuthDto;
import com.carspa.userservice.model.User;
import com.carspa.userservice.repository.UserRepository;
import com.carspa.userservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository     userRepository;
    @Mock PasswordEncoder    passwordEncoder;
    @Mock JwtUtil            jwtUtil;
    @Mock AuthenticationManager authManager;
    @Mock EmailService       emailService;

    @InjectMocks AuthService authService;

    private User testUser;

    @BeforeEach
    void setup() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Vaishnavi Patil");
        testUser.setEmail("vaishnavi@test.com");
        testUser.setPassword("$2a$12$hashed");
        testUser.setRoles(Set.of("ROLE_USER"));
        testUser.setActive(true);
    }

    // ── Register ──

    @Test
    void register_success_returnsToken() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setFullName("Vaishnavi Patil");
        req.setEmail("vaishnavi@test.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(userRepository.save(any())).thenReturn(testUser);
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("mock.jwt.token");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        AuthDto.TokenResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getEmail()).isEqualTo("vaishnavi@test.com");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
        // welcome email should be triggered
        verify(emailService).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void register_duplicateEmail_throwsException() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setEmail("vaishnavi@test.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail("vaishnavi@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    // ── Login ──

    @Test
    void login_validCredentials_returnsToken() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("vaishnavi@test.com");
        req.setPassword("password123");

        when(userRepository.findByEmail("vaishnavi@test.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("mock.jwt.token");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        AuthDto.TokenResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getFullName()).isEqualTo("Vaishnavi Patil");
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("vaishnavi@test.com");
        req.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
            .when(authManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(BadCredentialsException.class);
    }

    // ── Profile ──

    @Test
    void getProfile_returnsCorrectDetails() {
        when(userRepository.findByEmail("vaishnavi@test.com")).thenReturn(Optional.of(testUser));

        AuthDto.UserProfile profile = authService.getProfile("vaishnavi@test.com");

        assertThat(profile.getFullName()).isEqualTo("Vaishnavi Patil");
        assertThat(profile.getEmail()).isEqualTo("vaishnavi@test.com");
        assertThat(profile.isActive()).isTrue();
    }

    // ── Deactivate user ──

    @Test
    void deactivateUser_setsActiveFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        authService.deactivateUser(1L);

        assertThat(testUser.isActive()).isFalse();
        verify(userRepository).save(testUser);
    }
}