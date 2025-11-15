/**
 * Vehicle.java — a user's registered vehicle.
 *
 * Users can register multiple vehicles. When booking, the frontend
 * can pre-fill the vehicle number from this list instead of making
 * users type it each time.
 *
 * userId links to user-service — no foreign key constraint because
 * microservices don't share a database.
 */
package com.carspa.carservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // owner — from X-User-Id header (gateway injects this)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // e.g. MH12AB1234 — stored uppercase
    @Column(name = "vehicle_number", nullable = false, length = 20)
    private String vehicleNumber;

    // SEDAN | HATCHBACK | SUV | BIKE | TRUCK
    @Column(name = "vehicle_type", length = 20)
    private String vehicleType;

    // e.g. Maruti, Honda, Hyundai
    @Column(name = "brand", length = 50)
    private String brand;

    // e.g. Swift, City, Creta
    @Column(name = "model", length = 50)
    private String model;

    // e.g. White, Black, Silver
    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "is_active")
    private boolean active = true;

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
