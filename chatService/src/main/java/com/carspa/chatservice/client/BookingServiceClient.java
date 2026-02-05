/**
 * BookingServiceClient.java — Feign client for fetching live booking stats.
 *
 * Used by the ADMIN chat mode to give the AI real business context.
 * The gateway injects X-User-* headers but Feign bypasses the gateway
 * and calls booking-service directly via Eureka (lb://booking-service).
 *
 * If booking-service is down, the fallback returns null and the
 * admin chat still works — just without live booking data.
 */
package com.carspa.chatservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "booking-service", fallback = BookingServiceClientFallback.class)
public interface BookingServiceClient {

    // calls GET /api/bookings/admin/stats directly on booking-service
    @GetMapping("/api/bookings/admin/stats")
    Map<String, Object> getAdminStats();
}
