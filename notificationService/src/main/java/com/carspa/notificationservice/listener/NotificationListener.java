/**
 * NotificationListener.java — the heart of this service.
 *
 * Two @RabbitListener methods, one per queue.
 * Each receives a deserialized event object, checks the eventType string,
 * and delegates to the right email template method.
 *
 * Why separate methods instead of one?
 *   - BookingEvent and PaymentEvent have different fields
 *   - Jackson needs to know the target type at deserialization time
 *   - Two listeners = two independent consumer threads (better throughput)
 *
 * Error handling:
 *   - If the message can't be deserialized → logged + discarded (not requeued)
 *     (configured via factory.setDefaultRequeueRejected(false) in RabbitMQConfig)
 *   - If email sending fails → logged, method returns normally → message acked
 *     We don't want infinite retries for a bad email address
 */
package com.carspa.notificationservice.listener;

import com.carspa.notificationservice.model.BookingEvent;
import com.carspa.notificationservice.model.PaymentEvent;
import com.carspa.notificationservice.service.NotificationEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationEmailService emailService;

    /**
     * Listens on the booking queue.
     * Routes to the right email template based on eventType.
     */
    @RabbitListener(queues = "${notification.booking-queue}")
    public void handleBookingEvent(BookingEvent event) {
        if (event == null || event.getEventType() == null) {
            log.warn("Received null or incomplete booking event — skipping");
            return;
        }

        log.info("Booking event received: {} for booking #{} → {}",
            event.getEventType(), event.getBookingId(), event.getUserEmail());

        switch (event.getEventType()) {
            case "booking.confirmed"   -> emailService.sendBookingConfirmed(event);
            case "booking.cancelled"   -> emailService.sendBookingCancelled(event);
            case "booking.in_progress" -> emailService.sendBookingInProgress(event);
            case "booking.done"        -> emailService.sendBookingDone(event);
            default -> log.debug("No email template for booking event type: {}", event.getEventType());
        }
    }

    /**
     * Listens on the payment queue.
     * Routes to success or failed email.
     */
    @RabbitListener(queues = "${notification.payment-queue}")
    public void handlePaymentEvent(PaymentEvent event) {
        if (event == null || event.getEventType() == null) {
            log.warn("Received null or incomplete payment event — skipping");
            return;
        }

        log.info("Payment event received: {} for payment #{} → {}",
            event.getEventType(), event.getPaymentId(), event.getUserEmail());

        switch (event.getEventType()) {
            case "payment.success" -> emailService.sendPaymentSuccess(event);
            case "payment.failed"  -> emailService.sendPaymentFailed(event);
            default -> log.debug("No email template for payment event type: {}", event.getEventType());
        }
    }
}