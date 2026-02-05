package com.carspa.chatservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class BookingServiceClientFallback implements BookingServiceClient {

    @Override
    public Map<String, Object> getAdminStats() {
        log.warn("BookingServiceClient fallback triggered — booking-service may be down");
        return Collections.emptyMap();
    }
}
