package com.carspa.chatservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class PaymentServiceClientFallback implements PaymentServiceClient {

    @Override
    public Map<String, Object> getRevenueStats() {
        log.warn("PaymentServiceClient fallback triggered — payment-service may be down");
        return Collections.emptyMap();
    }
}
