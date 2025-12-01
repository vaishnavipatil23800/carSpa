/**
 * BookingRepository.java
 *
 * The isSlotTaken() query is the key logic for slot conflict prevention.
 * It checks if any ACTIVE booking (not cancelled) exists within a
 * configurable time window around the requested slot at the same wash centre.
 *
 * Example: if slot window = 30 min, and a booking exists at 10:00 AM,
 * then 9:45 AM, 10:00 AM, and 10:15 AM will all be rejected.
 */
package com.carspa.bookingservice.repository;

import com.carspa.bookingservice.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // user's own bookings, newest first
    List<Booking> findByUserIdOrderBySlotTimeDesc(Long userId);

    // admin — all bookings newest first
    List<Booking> findAllByOrderByCreatedAtDesc();

    /**
     * Checks whether a slot is already taken within the conflict window.
     * Excludes CANCELLED bookings — they free up the slot.
     *
     * :windowStart = requestedSlot - windowMinutes
     * :windowEnd   = requestedSlot + windowMinutes
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.washCentre = :washCentre
          AND b.status     <> 'CANCELLED'
          AND b.slotTime   BETWEEN :windowStart AND :windowEnd
        """)
    boolean isSlotTaken(
        @Param("washCentre")   String        washCentre,
        @Param("windowStart")  LocalDateTime windowStart,
        @Param("windowEnd")    LocalDateTime windowEnd
    );

    // for admin stats endpoint
    long countByStatus(String status);

    // for admin stats — bookings per service type
    @Query("SELECT b.serviceType, COUNT(b) FROM Booking b GROUP BY b.serviceType")
    List<Object[]> countByServiceType();
}
