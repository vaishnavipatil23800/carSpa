/**
 * BookingEvent.java — mirrors the shape published by booking-service.
 *
 * IMPORTANT: field names and types here MUST match BookingEvent in booking-service
 * exactly, otherwise Jackson deserialization will fail silently and
 * fields will be null.
 *
 * If you add a field to booking-service's BookingEvent,
 * add it here too before deploying.
 */
package com.carspa.notificationservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// @JsonIgnoreProperties(ignoreUnknown = true) means if booking-service
// adds new fields we don't know about, we won't crash — we just ignore them
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingEvent {

    private Long          bookingId;
    private Long          userId;
    private String        userEmail;
    private String        userName;
    private String        vehicleNumber;
    private String        serviceType;
    private String        washCentre;
    private LocalDateTime slotTime;

    // what triggered this: booking.confirmed | booking.cancelled |
    //                      booking.in_progress | booking.done | booking.updated
    private String        eventType;

    private String        status;
    private LocalDateTime occurredAt;
}
