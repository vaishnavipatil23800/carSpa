/**
 * WashCentre.java — a physical CarSpa location where washes happen.
 *
 * Admins create and manage wash centres.
 * Users see the list when booking and pick a location.
 * The centre name is used as the slot conflict key in booking-service.
 *
 * operatingHours is a simple string for now (e.g. "07:00-20:00").
 * TODO: replace with proper time-slot entity if we add time-based pricing
 */
package com.carspa.carservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wash_centres")
@Getter
@Setter
@NoArgsConstructor
public class WashCentre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 50)
    private String city;

    @Column(length = 10)
    private String pincode;

    // contact number for the centre
    @Column(length = 15)
    private String phone;

    // e.g. "07:00-20:00"
    @Column(name = "operating_hours", length = 30)
    private String operatingHours;

    // service pricing
    @Column(name = "price_basic", precision = 8, scale = 2)
    private BigDecimal priceBasic;      // BASIC wash price

    @Column(name = "price_premium", precision = 8, scale = 2)
    private BigDecimal pricePremium;    // PREMIUM wash price

    @Column(name = "price_full_detail", precision = 8, scale = 2)
    private BigDecimal priceFullDetail; // FULL_DETAIL price

    // rating out of 5.0
    @Column(precision = 3, scale = 1)
    private BigDecimal rating;

    // max concurrent washes at this centre
    @Column(name = "capacity")
    private Integer capacity = 3;

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
