/**
 * BookingService.java — all booking business logic.
 *
 * Key design decisions:
 * ─────────────────────
 * 1. Slot conflict check uses a DB query with a configurable time window.
 *    If two requests arrive simultaneously, the DB transaction + unique
 *    constraint prevents double-booking (transactional safety).
 *
 * 2. Booking events are published to RabbitMQ ASYNCHRONOUSLY (@Async).
 *    If RabbitMQ is down, the booking still succeeds — event is just logged
 *    as a warning. Don't want infrastructure issues breaking bookings.
 *
 * 3. User identity comes from the X-User-* headers the gateway injected.
 *    booking-service never touches a JWT directly.
 */
package com.carspa.bookingservice.service;

import com.carspa.bookingservice.config.RabbitMQConfig;
import com.carspa.bookingservice.dto.BookingDto;
import com.carspa.bookingservice.messaging.BookingEvent;
import com.carspa.bookingservice.model.Booking;
import com.carspa.bookingservice.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired
    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Value("${booking.slot-window-minutes:30}")
    private int slotWindowMinutes;

    // ── Create ──

    @Transactional
    public BookingDto.BookingResponse createBooking(
        BookingDto.BookingRequest request,
        Long   userId,
        String userEmail,
        String userName
    ) {
        // check slot conflict within the window at the same wash centre
        LocalDateTime windowStart = request.getSlotTime().minusMinutes(slotWindowMinutes);
        LocalDateTime windowEnd   = request.getSlotTime().plusMinutes(slotWindowMinutes);

        boolean taken = bookingRepository.isSlotTaken(
            request.getWashCentre(), windowStart, windowEnd
        );

        if (taken) {
            throw new IllegalStateException(
                "Slot at " + request.getWashCentre() +
                " around " + request.getSlotTime() +
                " is already booked. Please choose a different time."
            );
        }

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setUserEmail(userEmail);
        booking.setUserName(userName);
        booking.setVehicleNumber(request.getVehicleNumber().toUpperCase());
        booking.setServiceType(request.getServiceType().toUpperCase());
        booking.setWashCentre(request.getWashCentre());
        booking.setSlotTime(request.getSlotTime());
        booking.setStatus("CONFIRMED");   // auto-confirm for now

        Booking saved = bookingRepository.save(booking);
        log.info("Booking #{} created for user {} at {}", saved.getId(), userEmail, request.getSlotTime());

        // publish event async — notification-service will send a confirmation email
        publishEvent(saved, "booking.confirmed");

        return toResponse(saved);
    }

    // ── Read ──

    public List<BookingDto.BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderBySlotTimeDesc(userId)
            .stream().map(this::toResponse).toList();
    }

    public BookingDto.BookingResponse getBookingById(Long bookingId, Long userId) {
        Booking booking = findById(bookingId);
        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("Access denied to booking #" + bookingId);
        }
        return toResponse(booking);
    }

    // ── Cancel ──

    @Transactional
    public BookingDto.BookingResponse cancelBooking(Long bookingId, Long userId, String reason) {
        Booking booking = findById(bookingId);

        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("You can only cancel your own bookings");
        }
        if ("DONE".equals(booking.getStatus()) || "CANCELLED".equals(booking.getStatus())) {
            throw new IllegalStateException("Cannot cancel a booking with status: " + booking.getStatus());
        }
        if ("IN_PROGRESS".equals(booking.getStatus())) {
            throw new IllegalStateException("Cannot cancel a wash that has already started");
        }

        booking.setStatus("CANCELLED");
        booking.setCancellationReason(reason != null ? reason : "Cancelled by user");
        Booking cancelled = bookingRepository.save(booking);

        publishEvent(cancelled, "booking.cancelled");
        return toResponse(cancelled);
    }

    // ── Admin: Update status ──

    @Transactional
    public BookingDto.BookingResponse updateStatus(Long bookingId, String newStatus, String reason) {
        Booking booking = findById(bookingId);
        String  oldStatus = booking.getStatus();

        validateStatusTransition(oldStatus, newStatus);

        booking.setStatus(newStatus);
        if (reason != null) booking.setCancellationReason(reason);

        Booking updated = bookingRepository.save(booking);
        log.info("Booking #{} status: {} → {}", bookingId, oldStatus, newStatus);

        String eventType = switch (newStatus) {
            case "IN_PROGRESS" -> "booking.in_progress";
            case "DONE"        -> "booking.done";
            case "CANCELLED"   -> "booking.cancelled";
            default            -> "booking.updated";
        };
        publishEvent(updated, eventType);

        return toResponse(updated);
    }

    // ── Admin ──

    public List<BookingDto.BookingResponse> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(this::toResponse).toList();
    }

    public BookingDto.AdminStats getAdminStats() {
        long total      = bookingRepository.count();
        long pending    = bookingRepository.countByStatus("PENDING");
        long confirmed  = bookingRepository.countByStatus("CONFIRMED");
        long inProgress = bookingRepository.countByStatus("IN_PROGRESS");
        long done       = bookingRepository.countByStatus("DONE");
        long cancelled  = bookingRepository.countByStatus("CANCELLED");

        Map<String, Long> byServiceType = new HashMap<>();
        bookingRepository.countByServiceType().forEach(row ->
            byServiceType.put((String) row[0], (Long) row[1])
        );

        return BookingDto.AdminStats.builder()
            .total(total)
            .pending(pending)
            .confirmed(confirmed)
            .inProgress(inProgress)
            .done(done)
            .cancelled(cancelled)
            .byServiceType(byServiceType)
            .build();
    }

    // ── private helpers ──

    private Booking findById(Long id) {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Booking not found: #" + id));
    }

    private void validateStatusTransition(String from, String to) {
        boolean valid = switch (from) {
            case "PENDING"     -> List.of("CONFIRMED", "CANCELLED").contains(to);
            case "CONFIRMED"   -> List.of("IN_PROGRESS", "CANCELLED").contains(to);
            case "IN_PROGRESS" -> List.of("DONE").contains(to);
            default            -> false;
        };
        if (!valid) {
            throw new IllegalStateException(
                "Invalid status transition: " + from + " → " + to
            );
        }
    }

    @Async
    protected void publishEvent(Booking booking, String eventType) {
        try {
            BookingEvent event = BookingEvent.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .userEmail(booking.getUserEmail())
                .userName(booking.getUserName())
                .vehicleNumber(booking.getVehicleNumber())
                .serviceType(booking.getServiceType())
                .washCentre(booking.getWashCentre())
                .slotTime(booking.getSlotTime())
                .eventType(eventType)
                .status(booking.getStatus())
                .occurredAt(LocalDateTime.now())
                .build();

            if (rabbitTemplate != null) rabbitTemplate.convertAndSend(
                RabbitMQConfig.BOOKING_EXCHANGE,
                eventType,   // routing key = event type
                event
            );
            log.debug("Published event '{}' for booking #{}", eventType, booking.getId());
        } catch (Exception e) {
            // RabbitMQ being down should NOT fail the booking operation
            log.warn("Could not publish event '{}' for booking #{}: {}",
                eventType, booking.getId(), e.getMessage());
        }
    }

    private BookingDto.BookingResponse toResponse(Booking b) {
        return BookingDto.BookingResponse.builder()
            .id(b.getId())
            .userId(b.getUserId())
            .userEmail(b.getUserEmail())
            .userName(b.getUserName())
            .vehicleNumber(b.getVehicleNumber())
            .serviceType(b.getServiceType())
            .washCentre(b.getWashCentre())
            .slotTime(b.getSlotTime())
            .status(b.getStatus())
            .cancellationReason(b.getCancellationReason())
            .createdAt(b.getCreatedAt())
            .build();
    }
}