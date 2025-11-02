/**
 * JwtUtil.java — handles token creation and validation.
 *
 * Uses HS256 (HMAC-SHA256). The secret must be at least 32 characters
 * or the key size check will throw at startup.
 *
 * Role and userId are embedded as claims so downstream services (and the
 * gateway filter) can extract them without hitting the database.
 */
package com.carspa.userservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long      expirationMs;

    public JwtUtil(
        @Value("${jwt.secret}")     String secret,
        @Value("${jwt.expiration}") long   expirationMs
    ) {
        this.signingKey   = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email, Map<String, Object> extraClaims) {
        Date now = new Date();
        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(email)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + expirationMs))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String expectedEmail) {
        try {
            return extractEmail(token).equals(expectedEmail) && !isExpired(token);
        } catch (JwtException e) {
            log.debug("Token invalid: {}", e.getMessage());
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    // ── private ──

    private boolean isExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
