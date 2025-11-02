/**
 * SwaggerConfig.java — adds a Bearer token input box to Swagger UI.
 * Without this, Swagger can't test protected endpoints.
 * Visit: http://localhost:8081/swagger-ui.html after starting the service.
 */
package com.carspa.userservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
    title       = "CarSpa — User Service API",
    version     = "1.0",
    description = "Registration, login, JWT auth, and user management"
))
@SecurityScheme(
    name   = "bearerAuth",
    type   = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class SwaggerConfig {
    // no beans needed — annotations do all the work
}
