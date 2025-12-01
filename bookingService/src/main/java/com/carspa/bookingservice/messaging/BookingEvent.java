/**
 * BookingEvent.java — the message shape published to RabbitMQ.
 *
 * Kept as a flat POJO (no JPA annotations) — it gets serialised to JSON
 * by Jackson and consumed by notification-service.
 *
 * eventType drives what email notification-service sends:
 *   "booking.confirmed"   → confirmation email
 *   "booking.cancelled"   → cancellation email
 *   "booking.in_progress" → wash started email
 *   "booking.done"        → wash complete email
 */
package com.carspa.bookingservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent implements Serializable {
    private static final long serialVersionUID = 1L;


    private Long          bookingId;
    private Long          userId;
    private String        userEmail;
    private String        userName;
    private String        vehicleNumber;
    private String        serviceType;
    private String        washCentre;
    private LocalDateTime slotTime;

    // what triggered this event — drives email template selection
    private String        eventType;

    private String        status;
    private LocalDateTime occurredAt;
}