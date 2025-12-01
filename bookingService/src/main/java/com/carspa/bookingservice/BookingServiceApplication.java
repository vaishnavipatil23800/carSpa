/**
 * BookingServiceApplication.java
 *
 * Handles booking creation, cancellation, slot conflict checking,
 * status updates, and publishing events to RabbitMQ.
 *
 * Port:  8082
 * DB:    bookingdb (MySQL)
 *
 * Requires: Eureka (8761) + MySQL (3306)
 * Optional: RabbitMQ (5672) — if not running, events are skipped with a warning
 */
package com.carspa.bookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
@EnableAsync
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
