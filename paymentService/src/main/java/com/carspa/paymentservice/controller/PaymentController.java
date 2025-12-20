/**
 * PaymentController.java
 *
 * POST  /api/payments/create-order    — Step 1: create Razorpay order
 * POST  /api/payments/verify          — Step 3: verify signature → SUCCESS/FAILED
 * GET   /api/payments/my              — user's payment history
 * GET   /api/payments/{id}/invoice    — download PDF invoice
 * GET   /api/payments/admin/revenue   — admin revenue stats (used by chat-service)
 */
package com.carspa.paymentservice.controller;

import com.carspa.paymentservice.dto.PaymentDto;
import com.carspa.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment flow and invoice download")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    @Operation(summary = "Step 1 — Create Razorpay order and get orderId")
    public ResponseEntity<PaymentDto.OrderResponse> createOrder(
        @Valid @RequestBody            PaymentDto.CreateOrderRequest request,
        @RequestHeader("X-User-Id")    String userId,
        @RequestHeader("X-User-Email") String userEmail,
        @RequestHeader(value = "X-User-Name", required = false) String userName
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            paymentService.createOrder(request, Long.parseLong(userId), userEmail, userName)
        );
    }

    @PostMapping("/verify")
    @Operation(summary = "Step 3 — Verify Razorpay signature and confirm payment")
    public ResponseEntity<PaymentDto.PaymentResponse> verifyPayment(
        @Valid @RequestBody PaymentDto.VerifyPaymentRequest request
    ) {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }

    @GetMapping("/my")
    @Operation(summary = "Get payment history for the logged-in user")
    public ResponseEntity<List<PaymentDto.PaymentResponse>> getMyPayments(
        @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(paymentService.getMyPayments(Long.parseLong(userId)));
    }

    @GetMapping("/{id}/invoice")
    @Operation(summary = "Download PDF invoice for a payment")
    public ResponseEntity<Resource> downloadInvoice(
        @PathVariable               Long   id,
        @RequestHeader("X-User-Id") String userId
    ) {
        String invoicePath = paymentService.getInvoicePath(id, Long.parseLong(userId));
        File   invoiceFile = new File(invoicePath);

        if (!invoiceFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(invoiceFile);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"CarSpa-Invoice-INV-" +
                    String.format("%05d", id) + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(resource);
    }

    @GetMapping("/admin/revenue")
    @Operation(summary = "Admin — revenue statistics (also used by chat-service AI context)")
    public ResponseEntity<PaymentDto.RevenueStats> getRevenueStats() {
        return ResponseEntity.ok(paymentService.getRevenueStats());
    }
}
