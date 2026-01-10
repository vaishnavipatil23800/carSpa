/**
 * NotificationEmailServiceTest.java
 *
 * Tests that:
 *   - The listener correctly routes events to the right email method
 *   - Null / empty events don't cause NullPointerExceptions
 *   - The email service calls JavaMailSender for valid events
 *   - Unknown eventTypes are silently skipped (no crash)
 */
package com.carspa.notificationservice.service;

import com.carspa.notificationservice.listener.NotificationListener;
import com.carspa.notificationservice.model.BookingEvent;
import com.carspa.notificationservice.model.PaymentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEmailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock NotificationEmailService emailService;

    @InjectMocks NotificationListener notificationListener;

    // ── booking event helpers ──

    private BookingEvent makeBookingEvent(String eventType) {
        BookingEvent e = new BookingEvent();
        e.setBookingId(1L);
        e.setUserId(1L);
        e.setUserEmail("user@test.com");
        e.setUserName("Test User");
        e.setVehicleNumber("MH12AB1234");
        e.setServiceType("BASIC");
        e.setWashCentre("Pune Central");
        e.setSlotTime(LocalDateTime.now().plusDays(1));
        e.setEventType(eventType);
        e.setStatus("CONFIRMED");
        return e;
    }

    private PaymentEvent makePaymentEvent(String eventType) {
        PaymentEvent e = new PaymentEvent();
        e.setPaymentId(1L);
        e.setBookingId(1L);
        e.setUserId(1L);
        e.setUserEmail("user@test.com");
        e.setUserName("Test User");
        e.setVehicleNumber("MH12AB1234");
        e.setServiceType("BASIC");
        e.setTotalAmount(new BigDecimal("352.82"));
        e.setEventType(eventType);
        e.setStatus("SUCCESS");
        return e;
    }

    // ── booking listener routing ──

    @Test
    void bookingConfirmed_callsCorrectEmailMethod() {
        notificationListener.handleBookingEvent(makeBookingEvent("booking.confirmed"));
        verify(emailService).sendBookingConfirmed(any(BookingEvent.class));
    }

    @Test
    void bookingCancelled_callsCorrectEmailMethod() {
        notificationListener.handleBookingEvent(makeBookingEvent("booking.cancelled"));
        verify(emailService).sendBookingCancelled(any(BookingEvent.class));
    }

    @Test
    void bookingInProgress_callsCorrectEmailMethod() {
        notificationListener.handleBookingEvent(makeBookingEvent("booking.in_progress"));
        verify(emailService).sendBookingInProgress(any(BookingEvent.class));
    }

    @Test
    void bookingDone_callsCorrectEmailMethod() {
        notificationListener.handleBookingEvent(makeBookingEvent("booking.done"));
        verify(emailService).sendBookingDone(any(BookingEvent.class));
    }

    @Test
    void unknownBookingEventType_doesNotCallAnyEmailMethod() {
        notificationListener.handleBookingEvent(makeBookingEvent("booking.something_new"));
        verify(emailService, never()).sendBookingConfirmed(any());
        verify(emailService, never()).sendBookingCancelled(any());
        verify(emailService, never()).sendBookingInProgress(any());
        verify(emailService, never()).sendBookingDone(any());
    }

    @Test
    void nullBookingEvent_doesNotThrow() {
        // should log a warning and return — not throw NPE
        notificationListener.handleBookingEvent(null);
        verify(emailService, never()).sendBookingConfirmed(any());
    }

    // ── payment listener routing ──

    @Test
    void paymentSuccess_callsCorrectEmailMethod() {
        notificationListener.handlePaymentEvent(makePaymentEvent("payment.success"));
        verify(emailService).sendPaymentSuccess(any(PaymentEvent.class));
    }

    @Test
    void paymentFailed_callsCorrectEmailMethod() {
        notificationListener.handlePaymentEvent(makePaymentEvent("payment.failed"));
        verify(emailService).sendPaymentFailed(any(PaymentEvent.class));
    }

    @Test
    void nullPaymentEvent_doesNotThrow() {
        notificationListener.handlePaymentEvent(null);
        verify(emailService, never()).sendPaymentSuccess(any());
        verify(emailService, never()).sendPaymentFailed(any());
    }

    @Test
    void unknownPaymentEventType_doesNotCallAnyEmailMethod() {
        notificationListener.handlePaymentEvent(makePaymentEvent("payment.refund"));
        verify(emailService, never()).sendPaymentSuccess(any());
        verify(emailService, never()).sendPaymentFailed(any());
    }
}
