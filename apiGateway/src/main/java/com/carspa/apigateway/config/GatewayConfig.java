/**
 * GatewayConfig.java — registers custom filter beans with Spring Cloud Gateway.
 *
 * Spring Cloud Gateway needs filter factories to be registered as beans
 * with the exact name that matches what's in application.yml.
 * The name in yml is "JwtAuthenticationFilter" which Spring resolves to
 * the JwtAuthenticationFilter bean automatically via convention.
 *
 * This config also adds a global logging filter so every request is logged.
 */
package com.carspa.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class GatewayConfig {

    /**
     * Global logging filter — logs every request that hits the gateway.
     * Order(-1) means it runs before all other filters.
     */
    @Bean
    @Order(-1)
    public GlobalFilter requestLoggingFilter() {
        return (exchange, chain) -> {
            String method = exchange.getRequest().getMethod().name();
            String path   = exchange.getRequest().getURI().getPath();
            long   start  = System.currentTimeMillis();

            log.info("→ {} {}", method, path);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                int    status  = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;
                long   elapsed = System.currentTimeMillis() - start;
                log.info("← {} {} | {} | {}ms", method, path, status, elapsed);
            }));
        };
    }
}
