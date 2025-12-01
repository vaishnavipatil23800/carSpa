/**
 * Booking.java — core booking entity.
 *
 * Status flow:
 *   PENDING → CONFIRMED → IN_PROGRESS → DONE
 *                       ↘ CANCELLED (at any point before IN_PROGRESS)
 *
 * userId and userEmail are stored here so booking-service can work
 * independently without calling user-service for every query.
 * The gateway injects X-User-* headers so we never re-validate the JWT.
 */
package com.carspa.bookingservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // injected from X-User-Id header — no foreign key to user-service
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", nullable = false, length = 150)
    private String userEmail;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "vehicle_number", nullable = false, length = 20)
    private String vehicleNumber;

    // BASIC | PREMIUM | FULL_DETAIL
    @Column(name = "service_type", nullable = false, length = 30)
    private String serviceType;

    // the booked wash centre location
    @Column(name = "wash_centre", length = 100)
    private String washCentre;

    @Column(name = "slot_time", nullable = false)
    private LocalDateTime slotTime;

    // PENDING | CONFIRMED | IN_PROGRESS | DONE | CANCELLED
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

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
