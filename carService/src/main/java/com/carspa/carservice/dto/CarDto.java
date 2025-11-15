/**
 * CarDto.java — request/response DTOs for vehicle and wash centre endpoints.
 */
package com.carspa.carservice.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CarDto {

    // ── Vehicle requests ──

    @Getter @Setter
    public static class VehicleRequest {

        @NotBlank(message = "Vehicle number is required")
        @Pattern(regexp = "^[A-Z]{2}\\d{2}[A-Z]{1,2}\\d{4}$",
                 message = "Vehicle number must be like MH12AB1234")
        private String vehicleNumber;

        @NotBlank(message = "Vehicle type is required")
        private String vehicleType;   // SEDAN | HATCHBACK | SUV | BIKE | TRUCK

        private String brand;
        private String model;
        private String color;
    }

    // ── Vehicle response ──

    @Getter @Builder
    public static class VehicleResponse {
        private Long          id;
        private Long          userId;
        private String        vehicleNumber;
        private String        vehicleType;
        private String        brand;
        private String        model;
        private String        color;
        private boolean       active;
        private LocalDateTime createdAt;
    }

    // ── Wash centre requests ──

    @Getter @Setter
    public static class WashCentreRequest {

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        private String name;

        @NotBlank(message = "Address is required")
        private String address;

        @NotBlank(message = "City is required")
        private String city;

        private String pincode;
        private String phone;
        private String operatingHours;   // e.g. "07:00-20:00"

        @DecimalMin(value = "0.0")
        private BigDecimal priceBasic;

        @DecimalMin(value = "0.0")
        private BigDecimal pricePremium;

        @DecimalMin(value = "0.0")
        private BigDecimal priceFullDetail;

        @Min(1) @Max(20)
        private Integer capacity;
    }

    // ── Wash centre response ──

    @Getter @Builder
    public static class WashCentreResponse {
        private Long       id;
        private String     name;
        private String     address;
        private String     city;
        private String     pincode;
        private String     phone;
        private String     operatingHours;
        private BigDecimal priceBasic;
        private BigDecimal pricePremium;
        private BigDecimal priceFullDetail;
        private BigDecimal rating;
        private Integer    capacity;
        private boolean    active;
    }
}
