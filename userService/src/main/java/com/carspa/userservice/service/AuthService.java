/**
 * AuthService.java — core auth logic + implements UserDetailsService
 * so Spring Security can load users during JWT filter validation.
 *
 * loadUserByUsername() is called on every authenticated request — keeping it
 * a simple DB lookup, no extra logic.
 */
package com.carspa.userservice.service;

import com.carspa.userservice.dto.AuthDto;
import com.carspa.userservice.model.User;
import com.carspa.userservice.repository.UserRepository;
import com.carspa.userservice.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class AuthService implements UserDetailsService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil               jwtUtil;
    private final AuthenticationManager authManager;
    private final EmailService          emailService;

    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       @Lazy AuthenticationManager authManager,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil        = jwtUtil;
        this.authManager    = authManager;
        this.emailService   = emailService;
    }

    // ── Register ──

    @Transactional
    public AuthDto.TokenResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRoles(Set.of("ROLE_USER"));

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // fires async — register response doesn't wait for SMTP
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        return buildTokenResponse(user);
    }

    // ── Login ──

    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        // throws BadCredentialsException on wrong password — SecurityConfig handles the 401
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = findByEmail(request.getEmail());
        return buildTokenResponse(user);
    }

    // ── Profile ──

    public AuthDto.UserProfile getProfile(String email) {
        User user = findByEmail(email);
        return toProfile(user);
    }

    // ── Admin operations ──

    public List<AuthDto.UserProfile> getAllUsers() {
        return userRepository.findAll()
            .stream()
            .map(this::toProfile)
            .toList();
    }

    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setActive(false);
        userRepository.save(user);
        log.info("User {} deactivated", userId);
    }

    // ── Spring Security — called by JwtFilter on every request ──

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = findByEmail(email);
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .roles(user.getRoles().stream()
                .map(r -> r.replace("ROLE_", ""))
                .toArray(String[]::new))
            .build();
    }

    // ── private helpers ──

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("No user: " + email));
    }

    private AuthDto.TokenResponse buildTokenResponse(User user) {
        String role  = user.getRoles().stream().findFirst().orElse("ROLE_USER");
        String token = jwtUtil.generateToken(
            user.getEmail(),
            Map.of("role", role, "userId", user.getId(), "fullName", user.getFullName())
        );
        return new AuthDto.TokenResponse(
            token,
            jwtUtil.getExpirationMs() / 1000,
            user.getEmail(),
            user.getFullName(),
            role
        );
    }

    private AuthDto.UserProfile toProfile(User u) {
        return AuthDto.UserProfile.builder()
            .id(u.getId())
            .fullName(u.getFullName())
            .email(u.getEmail())
            .phone(u.getPhone())
            .role(u.getRoles().stream().findFirst().orElse("ROLE_USER"))
            .active(u.isActive())
            .build();
    }
}