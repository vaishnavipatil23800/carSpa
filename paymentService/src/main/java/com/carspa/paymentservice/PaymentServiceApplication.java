/**
 * PaymentServiceApplication.java
 *
 * Handles:
 *   - Razorpay order creation (step 1 of payment)
 *   - HMAC-SHA256 signature verification (step 2 — confirms payment)
 *   - iText 7 PDF invoice generation with GST breakdown
 *   - Invoice email delivery (async)
 *   - RabbitMQ event publishing to notification-service
 *   - Admin revenue reporting
 *
 * Port:  8084
 * DB:    paymentdb (MySQL)
 *
 * Requires: Eureka (8761) + MySQL (3306)
 * Optional: RabbitMQ + SMTP (if down, payments still work — events/email skipped)
 */
package com.carspa.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
