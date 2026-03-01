/**
 * JwtUtil.java (gateway) — validates tokens and extracts claims.
 *
 * The gateway does NOT issue tokens — that's user-service's job.
 * Here we only need to verify the signature and read the claims.
 *
 * Must use the same secret key as user-service or validation will fail.
 */
package com.carspa.apigateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(
            secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Returns true only if the token has a valid signature AND is not expired.
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            log.debug("Token expired");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        Object role = parseClaims(token).get("role");
        return role != null ? role.toString() : "";
    }

    public String extractUserId(String token) {
        Object userId = parseClaims(token).get("userId");
        return userId != null ? userId.toString() : "";
    }

    public String extractFullName(String token) {
        Object fullName = parseClaims(token).get("fullName");
        return fullName != null ? fullName.toString() : "";
    }

    // ── private ──

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
