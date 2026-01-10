/**
 * PaymentEvent.java — mirrors the shape published by payment-service.
 */
package com.carspa.notificationservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEvent {

    private Long          paymentId;
    private Long          bookingId;
    private Long          userId;
    private String        userEmail;
    private String        userName;
    private String        vehicleNumber;
    private String        serviceType;
    private String        washCentre;
    private BigDecimal    totalAmount;

    // payment.success | payment.failed
    private String        eventType;

    private String        status;
    private String        razorpayPaymentId;
    private LocalDateTime occurredAt;
}
