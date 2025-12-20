/**
 * PaymentServiceTest.java — unit tests for payment calculations and data access.
 *
 * We don't test the Razorpay API call directly (that's an integration test).
 * We test everything we own: GST maths, repository lookups, revenue stats, etc.
 */
package com.carspa.paymentservice.service;

import com.carspa.paymentservice.dto.PaymentDto;
import com.carspa.paymentservice.model.Payment;
import com.carspa.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository   paymentRepository;
    @Mock RabbitTemplate      rabbitTemplate;
    @Mock InvoiceService      invoiceService;
    @Mock InvoiceEmailService invoiceEmailService;

    @InjectMocks PaymentService paymentService;

    private Payment makePayment(Long id, String status) {
        Payment p = new Payment();
        p.setId(id);
        p.setBookingId(1L);
        p.setUserId(1L);
        p.setUserEmail("user@test.com");
        p.setUserName("Test User");
        p.setVehicleNumber("MH12AB1234");
        p.setServiceType("BASIC");
        p.setWashCentre("Pune Central");
        p.setAmount(new BigDecimal("299.00"));
        p.setGstAmount(new BigDecimal("53.82"));
        p.setTotalAmount(new BigDecimal("352.82"));
        p.setStatus(status);
        return p;
    }

    // ── getMyPayments ──

    @Test
    void getMyPayments_returnsUserPayments() {
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(List.of(makePayment(1L, "SUCCESS")));

        List<PaymentDto.PaymentResponse> result = paymentService.getMyPayments(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("SUCCESS");
    }

    // ── getPaymentById ──

    @Test
    void getPaymentById_correctUser_returnsPayment() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(makePayment(1L, "SUCCESS")));

        PaymentDto.PaymentResponse result = paymentService.getPaymentById(1L, 1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getPaymentById_wrongUser_throwsSecurityException() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(makePayment(1L, "SUCCESS")));

        assertThatThrownBy(() -> paymentService.getPaymentById(1L, 99L))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Access denied");
    }

    // ── getInvoicePath ──

    @Test
    void getInvoicePath_failedPayment_throwsIllegalState() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(makePayment(1L, "FAILED")));

        assertThatThrownBy(() -> paymentService.getInvoicePath(1L, 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("successful payments");
    }

    @Test
    void getInvoicePath_notFound_throwsRuntimeException() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getInvoicePath(999L, 1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");
    }

    // ── getRevenueStats ──

    @Test
    void getRevenueStats_returnsAggregatedData() {
        when(paymentRepository.getTotalRevenue()).thenReturn(new BigDecimal("1000.00"));
        when(paymentRepository.countByStatus("SUCCESS")).thenReturn(3L);
        when(paymentRepository.countByStatus("FAILED")).thenReturn(1L);
        List<Object[]> revenueData = new java.util.ArrayList<>();
        revenueData.add(new Object[]{"BASIC", new BigDecimal("600.00")});
        when(paymentRepository.getRevenueByServiceType()).thenReturn(revenueData);

        PaymentDto.RevenueStats stats = paymentService.getRevenueStats();

        assertThat(stats.getTotalRevenue()).isEqualByComparingTo("1000.00");
        assertThat(stats.getSuccessCount()).isEqualTo(3);
        assertThat(stats.getFailedCount()).isEqualTo(1);
        assertThat(stats.getRevenueByServiceType()).containsKey("BASIC");
    }

    // ── verifyPayment: order not found ──

    @Test
    void verifyPayment_orderNotFound_throwsRuntimeException() {
        when(paymentRepository.findByRazorpayOrderId("bad_order")).thenReturn(Optional.empty());

        PaymentDto.VerifyPaymentRequest req = new PaymentDto.VerifyPaymentRequest();
        req.setRazorpayOrderId("bad_order");
        req.setRazorpayPaymentId("pay_xxx");
        req.setRazorpaySignature("sig_xxx");

        assertThatThrownBy(() -> paymentService.verifyPayment(req))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");
    }
}