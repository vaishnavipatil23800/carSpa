/**
 * CarServiceApplication.java
 *
 * Manages two things:
 *   1. Vehicles — user's registered vehicles (plate number, type, brand)
 *   2. Wash Centres — available locations for booking (admin-managed)
 *
 * The booking-service uses the wash centre names returned here
 * to validate booking requests and check slot conflicts.
 *
 * Port:  8083
 * DB:    cardb (MySQL)
 *
 * Requires: Eureka (8761) + MySQL (3306)
 */
package com.carspa.carservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
public class CarServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarServiceApplication.class, args);
    }
}
