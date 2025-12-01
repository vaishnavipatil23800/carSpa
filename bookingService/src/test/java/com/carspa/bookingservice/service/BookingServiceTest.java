/**
 * BookingServiceTest.java — unit tests for core booking logic.
 * All dependencies mocked — no DB, Eureka, or RabbitMQ needed.
 */
package com.carspa.bookingservice.service;

import com.carspa.bookingservice.dto.BookingDto;
import com.carspa.bookingservice.model.Booking;
import com.carspa.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock RabbitTemplate    rabbitTemplate;

    @InjectMocks BookingService bookingService;

    @BeforeEach
    void setup() {
        // inject @Value field since Mockito won't do it automatically
        ReflectionTestUtils.setField(bookingService, "slotWindowMinutes", 30);
    }

    private Booking makeBooking(Long id, Long userId, String status) {
        Booking b = new Booking();
        b.setId(id);
        b.setUserId(userId);
        b.setUserEmail("user@test.com");
        b.setUserName("Test User");
        b.setVehicleNumber("MH12AB1234");
        b.setServiceType("BASIC");
        b.setWashCentre("Pune Central");
        b.setSlotTime(LocalDateTime.now().plusDays(1));
        b.setStatus(status);
        return b;
    }

    // ── Create ──

    @Test
    void createBooking_slotFree_success() {
        BookingDto.BookingRequest req = new BookingDto.BookingRequest();
        req.setVehicleNumber("MH12AB1234");
        req.setServiceType("BASIC");
        req.setWashCentre("Pune Central");
        req.setSlotTime(LocalDateTime.now().plusDays(1));

        Booking saved = makeBooking(1L, 1L, "CONFIRMED");

        when(bookingRepository.isSlotTaken(anyString(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any())).thenReturn(saved);

        BookingDto.BookingResponse response =
            bookingService.createBooking(req, 1L, "user@test.com", "Test User");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_slotTaken_throwsConflict() {
        BookingDto.BookingRequest req = new BookingDto.BookingRequest();
        req.setVehicleNumber("MH12AB1234");
        req.setServiceType("BASIC");
        req.setWashCentre("Pune Central");
        req.setSlotTime(LocalDateTime.now().plusDays(1));

        when(bookingRepository.isSlotTaken(anyString(), any(), any())).thenReturn(true);

        assertThatThrownBy(() ->
            bookingService.createBooking(req, 1L, "user@test.com", "Test User")
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("already booked");

        verify(bookingRepository, never()).save(any());
    }

    // ── Cancel ──

    @Test
    void cancelBooking_confirmedBooking_success() {
        Booking booking = makeBooking(1L, 1L, "CONFIRMED");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenReturn(booking);

        BookingDto.BookingResponse result =
            bookingService.cancelBooking(1L, 1L, "Change of plans");

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getCancellationReason()).isEqualTo("Change of plans");
    }

    @Test
    void cancelBooking_wrongUser_throwsSecurityException() {
        Booking booking = makeBooking(1L, 1L, "CONFIRMED");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // user 2 trying to cancel user 1's booking
        assertThatThrownBy(() -> bookingService.cancelBooking(1L, 2L, ""))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("your own");
    }

    @Test
    void cancelBooking_alreadyDone_throwsIllegalState() {
        Booking booking = makeBooking(1L, 1L, "DONE");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L, 1L, ""))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DONE");
    }

    // ── Status transition ──

    @Test
    void updateStatus_confirmedToInProgress_success() {
        Booking booking = makeBooking(1L, 1L, "CONFIRMED");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenReturn(booking);

        BookingDto.BookingResponse result =
            bookingService.updateStatus(1L, "IN_PROGRESS", null);

        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void updateStatus_invalidTransition_throwsIllegalState() {
        Booking booking = makeBooking(1L, 1L, "DONE");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.updateStatus(1L, "CONFIRMED", null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid status transition");
    }

    // ── Get user bookings ──

    @Test
    void getUserBookings_returnsOnlyUserBookings() {
        when(bookingRepository.findByUserIdOrderBySlotTimeDesc(1L))
            .thenReturn(List.of(
                makeBooking(1L, 1L, "CONFIRMED"),
                makeBooking(2L, 1L, "DONE")
            ));

        List<BookingDto.BookingResponse> results = bookingService.getUserBookings(1L);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.getUserId().equals(1L));
    }
}
