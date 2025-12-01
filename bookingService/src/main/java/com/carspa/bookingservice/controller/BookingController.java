/**
 * BookingController.java — all booking endpoints.
 *
 * User identity is read from X-User-* headers injected by the gateway.
 * This service never touches a JWT directly — the gateway already validated it.
 *
 * Endpoints:
 *   POST   /api/bookings              — create booking
 *   GET    /api/bookings              — my bookings
 *   GET    /api/bookings/{id}         — single booking
 *   PATCH  /api/bookings/{id}/cancel  — cancel my booking
 *   PATCH  /api/bookings/{id}/status  — admin: update status
 *   GET    /api/bookings/admin/all    — admin: all bookings
 *   GET    /api/bookings/admin/stats  — admin: stats (used by chat-service)
 */
package com.carspa.bookingservice.controller;

import com.carspa.bookingservice.dto.BookingDto;
import com.carspa.bookingservice.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking creation, cancellation, and status management")
public class BookingController {

    private final BookingService bookingService;

    // ── User endpoints ──

    @PostMapping
    @Operation(summary = "Create a new booking")
    public ResponseEntity<BookingDto.BookingResponse> createBooking(
        @Valid @RequestBody           BookingDto.BookingRequest request,
        @RequestHeader("X-User-Id")   String userId,
        @RequestHeader("X-User-Email")String userEmail,
        @RequestHeader(value = "X-User-Name", required = false) String userName
    ) {
        BookingDto.BookingResponse response = bookingService.createBooking(
            request, Long.parseLong(userId), userEmail, userName
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all bookings for the logged-in user")
    public ResponseEntity<List<BookingDto.BookingResponse>> getMyBookings(
        @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(bookingService.getUserBookings(Long.parseLong(userId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single booking by ID")
    public ResponseEntity<BookingDto.BookingResponse> getBooking(
        @PathVariable              Long   id,
        @RequestHeader("X-User-Id")String userId
    ) {
        return ResponseEntity.ok(bookingService.getBookingById(id, Long.parseLong(userId)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<BookingDto.BookingResponse> cancelBooking(
        @PathVariable              Long   id,
        @RequestHeader("X-User-Id")String userId,
        @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, Long.parseLong(userId), reason));
    }

    // ── Admin endpoints ──

    @GetMapping("/admin/all")
    @Operation(summary = "Admin — list all bookings")
    public ResponseEntity<List<BookingDto.BookingResponse>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/admin/stats")
    @Operation(summary = "Admin — booking statistics (also used by chat-service)")
    public ResponseEntity<BookingDto.AdminStats> getAdminStats() {
        return ResponseEntity.ok(bookingService.getAdminStats());
    }

    @PatchMapping("/admin/{id}/status")
    @Operation(summary = "Admin — update booking status")
    public ResponseEntity<BookingDto.BookingResponse> updateStatus(
        @PathVariable                           Long   id,
        @Valid @RequestBody BookingDto.UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(
            bookingService.updateStatus(id, request.getStatus(), request.getReason())
        );
    }
}
