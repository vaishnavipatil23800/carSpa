/**
 * BookingDto.java — request/response DTOs for booking endpoints.
 */
package com.carspa.bookingservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

public class BookingDto {

    // ── Inbound ──

    @Getter @Setter
    public static class BookingRequest {

        @NotBlank(message = "Vehicle number is required")
        @Pattern(regexp = "^[A-Z]{2}\\d{2}[A-Z]{1,2}\\d{4}$",
                 message = "Vehicle number must be like MH12AB1234")
        private String vehicleNumber;

        @NotBlank(message = "Service type is required")
        // BASIC | PREMIUM | FULL_DETAIL
        private String serviceType;

        @NotBlank(message = "Wash centre is required")
        private String washCentre;

        @NotNull(message = "Slot time is required")
        @Future(message = "Slot time must be in the future")
        private LocalDateTime slotTime;
    }

    @Getter @Setter
    public static class UpdateStatusRequest {
        @NotBlank(message = "Status is required")
        private String status;  // CONFIRMED | IN_PROGRESS | DONE | CANCELLED

        private String reason;  // optional cancellation reason
    }

    // ── Outbound ──

    @Getter @Builder
    public static class BookingResponse {
        private Long          id;
        private Long          userId;
        private String        userEmail;
        private String        userName;
        private String        vehicleNumber;
        private String        serviceType;
        private String        washCentre;
        private LocalDateTime slotTime;
        private String        status;
        private String        cancellationReason;
        private LocalDateTime createdAt;
    }

    // used by the admin stats endpoint + chat-service context
    @Getter @Builder
    public static class AdminStats {
        private long total;
        private long pending;
        private long confirmed;
        private long inProgress;
        private long done;
        private long cancelled;
        private Map<String, Long> byServiceType;
    }
}
