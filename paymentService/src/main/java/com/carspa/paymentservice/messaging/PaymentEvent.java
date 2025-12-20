package com.carspa.paymentservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent implements Serializable {
    private static final long serialVersionUID = 1L;


    private Long       paymentId;
    private Long       bookingId;
    private Long       userId;
    private String     userEmail;
    private String     userName;
    private String     vehicleNumber;
    private String     serviceType;
    private String     washCentre;
    private BigDecimal totalAmount;

    // "payment.success" or "payment.failed"
    private String     eventType;

    private String     status;
    private String     razorpayPaymentId;
    private LocalDateTime occurredAt;
}