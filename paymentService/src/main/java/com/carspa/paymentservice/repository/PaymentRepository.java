package com.carspa.paymentservice.repository;

import com.carspa.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    List<Payment> findByStatus(String status);

    // total revenue from successful payments
    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal getTotalRevenue();

    // revenue grouped by service type
    @Query("SELECT p.serviceType, COALESCE(SUM(p.totalAmount), 0) FROM Payment p WHERE p.status = 'SUCCESS' GROUP BY p.serviceType")
    List<Object[]> getRevenueByServiceType();

    long countByStatus(String status);
}
