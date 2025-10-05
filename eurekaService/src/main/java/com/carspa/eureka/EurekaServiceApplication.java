/**
 * EurekaServiceApplication.java
 *
 * The service registry — every other CarSpa microservice registers here.
 * Without this running, no service can discover another.
 *
 * Port:  8761
 * Usage: mvn spring-boot:run
 */
package com.carspa.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServiceApplication.class, args);
    }
}
