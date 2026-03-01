/**
 * JwtUtilTest.java — tests token validation logic in the gateway filter.
 * No Spring context needed — just plain unit tests.
 */
package com.carspa.apigateway;

import com.carspa.apigateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET =
        "carspa-local-dev-secret-key-min-32-chars-change-in-prod";

    private JwtUtil jwtUtil;
    private SecretKey signingKey;

    @BeforeEach
    void setup() {
        jwtUtil    = new JwtUtil(SECRET);
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String buildToken(long expiryMs) {
        return Jwts.builder()
            .setSubject("test@carspa.com")
            .setClaims(Map.of(
                "sub",      "test@carspa.com",
                "role",     "ROLE_USER",
                "userId",   "1",
                "fullName", "Test User"
            ))
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    @Test
    void validToken_isValid() {
        String token = buildToken(3_600_000); // 1 hour
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void expiredToken_isInvalid() {
        String token = buildToken(-1000); // already expired
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = buildToken(3_600_000);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("test@carspa.com");
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token = buildToken(3_600_000);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_USER");
    }

    @Test
    void extractUserId_returnsCorrectId() {
        String token = buildToken(3_600_000);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("1");
    }

    @Test
    void randomString_isInvalid() {
        assertThat(jwtUtil.isTokenValid("this.is.not.a.token")).isFalse();
    }
}
