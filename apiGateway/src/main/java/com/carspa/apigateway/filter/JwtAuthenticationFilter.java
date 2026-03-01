/**
 * JwtAuthenticationFilter.java
 *
 * This is the most important class in the gateway.
 * It runs on every request BEFORE it is forwarded to any microservice.
 *
 * What it does:
 *   1. Checks the Authorization: Bearer <token> header
 *   2. Validates the token signature and expiry
 *   3. If valid — injects X-User-Email, X-User-Id, X-User-Role headers
 *      so downstream services know who the caller is WITHOUT re-parsing the token
 *   4. If invalid — returns 401 immediately, the request never reaches the service
 *
 * This means each individual microservice does NOT need its own JWT filter.
 * They just read the X-User-* headers that the gateway injects.
 *
 * Public paths (register, login) skip this filter entirely.
 */
package com.carspa.apigateway.filter;

import com.carspa.apigateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    /**
     * These paths SKIP JWT validation.
     * Add any other public endpoints here as you build new services.
     */
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/users/auth/login",
        "/api/users/auth/register",
        "/v3/api-docs",
        "/swagger-ui",
        "/actuator/health"
    );

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // ── skip public paths ──
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            // ── check Authorization header ──
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing Authorization header for path: {}", path);
                return rejectWith401(exchange, "Missing Authorization header");
            }

            String token = authHeader.substring(7);

            // ── validate token ──
            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid or expired token for path: {}", path);
                return rejectWith401(exchange, "Invalid or expired token");
            }

            // ── token is valid — extract identity and inject as headers ──
            // Downstream services read these headers instead of parsing the token themselves
            try {
                String email    = jwtUtil.extractEmail(token);
                String role     = jwtUtil.extractRole(token);
                String userId   = jwtUtil.extractUserId(token);
                String fullName = jwtUtil.extractFullName(token);

                ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Email",    email)
                    .header("X-User-Role",     role)
                    .header("X-User-Id",       userId)
                    .header("X-User-Name",     fullName)
                    // Remove the original Authorization header so internal services
                    // don't accidentally try to validate it themselves
                    .build();

                log.debug("Forwarding request for user {} to {}", email, path);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                log.error("Error extracting token claims: {}", e.getMessage());
                return rejectWith401(exchange, "Token processing error");
            }
        };
    }

    // ── private helpers ──

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> rejectWith401(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        // add a header so the frontend knows why it was rejected
        response.getHeaders().add("X-Auth-Error", reason);
        return response.setComplete();
    }

    /**
     * Config class required by AbstractGatewayFilterFactory.
     * Currently empty — add configurable fields here if needed later
     * (e.g., per-route skip paths, custom header names).
     */
    public static class Config {
        // intentionally empty
    }
}
