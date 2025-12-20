/**
 * PaymentService.java — the full Razorpay payment flow.
 *
 * HOW RAZORPAY WORKS (test mode):
 * ────────────────────────────────
 * Step 1 — createOrder()
 *   → We create a Razorpay order (returns orderId like "order_xxxxxxxxxx")
 *   → Save Payment record with status=CREATED
 *   → Frontend uses orderId + key_id to open the Razorpay payment modal
 *
 * Step 2 — User pays in the frontend modal (test mode: use card 4111 1111 1111 1111)
 *   → Razorpay returns: razorpayPaymentId + razorpaySignature to the frontend
 *
 * Step 3 — verifyPayment()
 *   → We build expected_signature = HMAC-SHA256(orderId + "|" + paymentId, keySecret)
 *   → If it matches razorpaySignature → payment is genuine → mark SUCCESS
 *   → Generate PDF invoice, email it, publish RabbitMQ event
 */
package com.carspa.paymentservice.service;

import com.carspa.paymentservice.config.RabbitMQConfig;
import com.carspa.paymentservice.dto.PaymentDto;
import com.carspa.paymentservice.messaging.PaymentEvent;
import com.carspa.paymentservice.model.Payment;
import com.carspa.paymentservice.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository    paymentRepository;
    private final RabbitTemplate       rabbitTemplate;
    private final InvoiceService       invoiceService;
    private final InvoiceEmailService  invoiceEmailService;

    @Value("${razorpay.key-id}")       private String razorpayKeyId;
    @Value("${razorpay.key-secret}")   private String razorpayKeySecret;
    @Value("${payment.gst-rate:0.18}") private double gstRate;

    public PaymentService(
        PaymentRepository   paymentRepository,
        RabbitTemplate      rabbitTemplate,
        InvoiceService      invoiceService,
        InvoiceEmailService invoiceEmailService
    ) {
        this.paymentRepository   = paymentRepository;
        this.rabbitTemplate      = rabbitTemplate;
        this.invoiceService      = invoiceService;
        this.invoiceEmailService = invoiceEmailService;
    }

    // ── Step 1: create Razorpay order ──

    @Transactional
    public PaymentDto.OrderResponse createOrder(
        PaymentDto.CreateOrderRequest request,
        Long   userId,
        String userEmail,
        String userName
    ) {
        // calculate GST
        BigDecimal base  = request.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal gst   = base.multiply(BigDecimal.valueOf(gstRate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = base.add(gst);

        // Razorpay expects amount in paise (1 INR = 100 paise)
        int amountInPaise = total.multiply(BigDecimal.valueOf(100)).intValue();

        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount",   amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt",  "booking_" + request.getBookingId());

            Order rzpOrder = razorpay.orders.create(orderRequest);
            String rzpOrderId = rzpOrder.get("id");

            // save payment record
            Payment payment = new Payment();
            payment.setBookingId(request.getBookingId());
            payment.setUserId(userId);
            payment.setUserEmail(userEmail);
            payment.setUserName(userName);
            payment.setVehicleNumber(request.getVehicleNumber());
            payment.setServiceType(request.getServiceType());
            payment.setWashCentre(request.getWashCentre());
            payment.setAmount(base);
            payment.setGstAmount(gst);
            payment.setTotalAmount(total);
            payment.setRazorpayOrderId(rzpOrderId);
            payment.setStatus("CREATED");

            Payment saved = paymentRepository.save(payment);
            log.info("Razorpay order created: {} for booking #{}", rzpOrderId, request.getBookingId());

            return PaymentDto.OrderResponse.builder()
                .paymentId(saved.getId())
                .razorpayOrderId(rzpOrderId)
                .currency("INR")
                .baseAmount(base)
                .gstAmount(gst)
                .totalAmount(total)
                .amountInPaise(amountInPaise)
                .status("CREATED")
                .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new RuntimeException("Payment gateway error: " + e.getMessage());
        }
    }

    // ── Step 3: verify signature and mark SUCCESS ──

    @Transactional
    public PaymentDto.PaymentResponse verifyPayment(PaymentDto.VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
            .orElseThrow(() -> new RuntimeException("Payment not found for order: " + request.getRazorpayOrderId()));

        // HMAC-SHA256 verification
        boolean valid = verifyHmacSignature(
            request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(),
            request.getRazorpaySignature()
        );

        if (valid) {
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setStatus("SUCCESS");
            Payment saved = paymentRepository.save(payment);

            log.info("Payment verified: {} for booking #{}", request.getRazorpayPaymentId(), saved.getBookingId());

            // generate PDF invoice + send email + publish event — all async/safe
            generateAndEmailInvoice(saved);
            publishEvent(saved, "payment.success");

            return toResponse(saved);

        } else {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            log.warn("Payment signature verification FAILED for order: {}", request.getRazorpayOrderId());
            publishEvent(payment, "payment.failed");
            throw new RuntimeException("Payment signature verification failed. Payment marked as FAILED.");
        }
    }

    // ── Read ──

    public List<PaymentDto.PaymentResponse> getMyPayments(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(this::toResponse).toList();
    }

    public PaymentDto.PaymentResponse getPaymentById(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found: #" + paymentId));
        if (!payment.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to payment #" + paymentId);
        }
        return toResponse(payment);
    }

    // returns PDF file path for streaming back to the client
    public String getInvoicePath(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found: #" + paymentId));
        if (!payment.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new IllegalStateException("Invoice only available for successful payments");
        }
        if (payment.getInvoicePath() == null) {
            // regenerate on-demand if path was lost
            try {
                String path = invoiceService.generateInvoice(payment);
                payment.setInvoicePath(path);
                paymentRepository.save(payment);
                return path;
            } catch (Exception e) {
                throw new RuntimeException("Could not generate invoice: " + e.getMessage());
            }
        }
        return payment.getInvoicePath();
    }

    // ── Admin ──

    public PaymentDto.RevenueStats getRevenueStats() {
        BigDecimal total    = paymentRepository.getTotalRevenue();
        long success        = paymentRepository.countByStatus("SUCCESS");
        long failed         = paymentRepository.countByStatus("FAILED");

        Map<String, BigDecimal> byType = new HashMap<>();
        paymentRepository.getRevenueByServiceType()
            .forEach(row -> byType.put((String) row[0], (BigDecimal) row[1]));

        return PaymentDto.RevenueStats.builder()
            .totalRevenue(total)
            .successCount(success)
            .failedCount(failed)
            .revenueByServiceType(byType)
            .build();
    }

    // ── private helpers ──

    private boolean verifyHmacSignature(String data, String expectedSignature) {
        try {
            Mac          mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(
                razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(key);
            byte[] hash          = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String computedSig   = HexFormat.of().formatHex(hash);
            return computedSig.equals(expectedSignature);
        } catch (Exception e) {
            log.error("HMAC verification error: {}", e.getMessage());
            return false;
        }
    }

    @Async
    protected void generateAndEmailInvoice(Payment payment) {
        try {
            String path = invoiceService.generateInvoice(payment);
            payment.setInvoicePath(path);
            paymentRepository.save(payment);
            invoiceEmailService.sendInvoiceEmail(payment, path);
        } catch (Exception e) {
            log.warn("Invoice generation/email error for payment #{}: {}", payment.getId(), e.getMessage());
        }
    }

    @Async
    protected void publishEvent(Payment payment, String eventType) {
        try {
            PaymentEvent event = PaymentEvent.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .userEmail(payment.getUserEmail())
                .userName(payment.getUserName())
                .vehicleNumber(payment.getVehicleNumber())
                .serviceType(payment.getServiceType())
                .washCentre(payment.getWashCentre())
                .totalAmount(payment.getTotalAmount())
                .eventType(eventType)
                .status(payment.getStatus())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .occurredAt(LocalDateTime.now())
                .build();

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EXCHANGE, eventType, event
            );
            log.debug("Published '{}' for payment #{}", eventType, payment.getId());
        } catch (Exception e) {
            log.warn("Could not publish payment event '{}': {}", eventType, e.getMessage());
        }
    }

    private PaymentDto.PaymentResponse toResponse(Payment p) {
        return PaymentDto.PaymentResponse.builder()
            .id(p.getId())
            .bookingId(p.getBookingId())
            .userId(p.getUserId())
            .userEmail(p.getUserEmail())
            .vehicleNumber(p.getVehicleNumber())
            .serviceType(p.getServiceType())
            .washCentre(p.getWashCentre())
            .amount(p.getAmount())
            .gstAmount(p.getGstAmount())
            .totalAmount(p.getTotalAmount())
            .razorpayOrderId(p.getRazorpayOrderId())
            .razorpayPaymentId(p.getRazorpayPaymentId())
            .status(p.getStatus())
            .createdAt(p.getCreatedAt())
            .build();
    }
}