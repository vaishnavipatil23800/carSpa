/**
 * BUG FIX: The original test had no webEnvironment setting.
 * Spring Boot defaults to MOCK which doesn't work for WebFlux (reactive) apps.
 * RANDOM_PORT starts the full reactive Netty server on a random port for tests.
 */
package com.carspa.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full gateway context (routes, filters, Netty server) starts cleanly
    }
}
