/**
 * Payment.java — records each payment attempt.
 *
 * Razorpay flow:
 *   1. Client calls /create-order → we create a Razorpay order, save Payment with CREATED status
 *   2. User pays on frontend using Razorpay JS SDK
 *   3. Client calls /verify → we check HMAC signature, mark payment SUCCESS or FAILED
 *
 * We store both the razorpayOrderId (step 1) and razorpayPaymentId (step 3)
 * so we can look up any payment in the Razorpay dashboard.
 */
package com.carspa.paymentservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // link to booking — no FK (different DB)
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Column(name = "user_name", length = 100)
    private String userName;

    // vehicle and service details (denormalised for invoice)
    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(name = "service_type", length = 30)
    private String serviceType;

    @Column(name = "wash_centre", length = 100)
    private String washCentre;

    // amount in INR (paise stored here, converted for display)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "gst_amount", precision = 10, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Razorpay identifiers
    @Column(name = "razorpay_order_id", length = 50)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 50)
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature", length = 255)
    private String razorpaySignature;

    // CREATED | SUCCESS | FAILED
    @Column(nullable = false, length = 15)
    private String status = "CREATED";

    @Column(name = "invoice_path", length = 255)
    private String invoicePath;   // local path to generated PDF

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
