/**
 * PaymentDto.java — request/response shapes for the payment flow.
 *
 * Razorpay payment flow:
 *   Step 1 — Frontend sends CreateOrderRequest → backend creates Razorpay order
 *             → frontend gets back OrderResponse with razorpayOrderId
 *   Step 2 — User pays using Razorpay JS SDK on the frontend
 *   Step 3 — Razorpay calls webhook / frontend gets razorpayPaymentId + signature
 *             → frontend sends VerifyPaymentRequest to our /verify endpoint
 *             → backend checks HMAC and marks payment SUCCESS or FAILED
 */
package com.carspa.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class PaymentDto {

    // ── Step 1: create Razorpay order ──

    @Getter @Setter
    public static class CreateOrderRequest {

        @NotNull(message = "Booking ID is required")
        private Long bookingId;

        @NotBlank(message = "Vehicle number is required")
        private String vehicleNumber;

        @NotBlank(message = "Service type is required")
        private String serviceType;   // BASIC | PREMIUM | FULL_DETAIL

        @NotBlank(message = "Wash centre is required")
        private String washCentre;

        @NotNull @Positive(message = "Amount must be positive")
        private BigDecimal amount;    // base amount in INR (before GST)
    }

    @Getter @Builder
    public static class OrderResponse {
        private Long       paymentId;       // our DB id
        private String     razorpayOrderId; // send this to Razorpay JS SDK
        private String     currency;        // "INR"
        private BigDecimal baseAmount;
        private BigDecimal gstAmount;
        private BigDecimal totalAmount;     // what user pays
        private int        amountInPaise;   // Razorpay works in paise
        private String     status;
    }

    // ── Step 3: verify payment ──

    @Getter @Setter
    public static class VerifyPaymentRequest {
        @NotBlank private String razorpayOrderId;
        @NotBlank private String razorpayPaymentId;
        @NotBlank private String razorpaySignature;   // HMAC-SHA256 we verify
    }

    // ── Generic payment response ──

    @Getter @Builder
    public static class PaymentResponse {
        private Long          id;
        private Long          bookingId;
        private Long          userId;
        private String        userEmail;
        private String        vehicleNumber;
        private String        serviceType;
        private String        washCentre;
        private BigDecimal    amount;
        private BigDecimal    gstAmount;
        private BigDecimal    totalAmount;
        private String        razorpayOrderId;
        private String        razorpayPaymentId;
        private String        status;
        private LocalDateTime createdAt;
    }

    // ── Admin revenue report ──

    @Getter @Builder
    public static class RevenueStats {
        private BigDecimal          totalRevenue;
        private long                successCount;
        private long                failedCount;
        private Map<String, BigDecimal> revenueByServiceType;
    }
}
