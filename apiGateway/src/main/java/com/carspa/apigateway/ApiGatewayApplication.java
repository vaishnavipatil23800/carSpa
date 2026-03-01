/**
 * ApiGatewayApplication.java
 *
 * The single entry point for ALL frontend requests.
 * Validates JWT tokens, then routes requests to the correct service.
 *
 * Port:  8080
 * Usage: mvn spring-boot:run
 *
 * Requires: Eureka (8761) + User Service (8081) running first.
 * Other services are optional — gateway will return 503 if a service is down.
 */
package com.carspa.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
