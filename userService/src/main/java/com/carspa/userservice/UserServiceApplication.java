/**
 * UserServiceApplication.java
 *
 * Handles user registration, login, JWT issuance,
 * profile management, and admin user operations.
 *
 * Port:  8081
 * Usage: mvn spring-boot:run
 *
 * Requires: Eureka (8761) + MySQL (3306) running first
 */
package com.carspa.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync  // for async welcome email sending
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
