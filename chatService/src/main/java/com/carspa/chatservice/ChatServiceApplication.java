/**
 * ChatServiceApplication.java
 *
 * AI chat assistant with two modes:
 *
 *   USER mode  → friendly support bot. Answers questions about CarSpa services,
 *                booking status, pricing, how to cancel, etc.
 *                Fetches user's own booking history for personalised answers.
 *
 *   ADMIN mode → business intelligence bot. Has access to live booking stats
 *                and revenue data via Feign calls to booking-service and
 *                payment-service. Can answer "how many bookings today?",
 *                "which service type generates most revenue?" etc.
 *
 * Conversation history is maintained client-side and sent with each request
 * (last N pairs of messages). The service is stateless — no DB needed.
 *
 * Port:  8086
 * DB:    none — stateless
 *
 * Requires: Eureka (8761) + valid OpenAI API key
 * Optional: booking-service + payment-service for admin context
 */
package com.carspa.chatservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }
}
