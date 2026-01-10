/**
 * NotificationServiceApplication.java
 *
 * This service is a pure RabbitMQ consumer — it has NO REST endpoints.
 * It listens on two queues and sends HTML emails accordingly:
 *
 *   carspa.booking.queue  → booking.confirmed, booking.cancelled,
 *                           booking.in_progress, booking.done
 *
 *   carspa.payment.queue  → payment.success, payment.failed
 *
 * Port:  8085  (web is kept alive so Eureka registration + actuator work)
 * DB:    none — stateless, just email delivery
 *
 * Requires: Eureka (8761) + RabbitMQ (5672) + SMTP configured
 */
package com.carspa.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
