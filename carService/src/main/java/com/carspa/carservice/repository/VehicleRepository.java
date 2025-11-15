package com.carspa.carservice.repository;

import com.carspa.carservice.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // all active vehicles for a user
    List<Vehicle> findByUserIdAndActiveTrue(Long userId);

    // check if the user already registered this plate
    boolean existsByUserIdAndVehicleNumber(Long userId, String vehicleNumber);

    Optional<Vehicle> findByIdAndUserId(Long id, Long userId);
}
