/**
 * VehicleService.java — vehicle CRUD for logged-in users.
 *
 * Users can only see and manage their own vehicles.
 * Identity comes from X-User-Id header (gateway injected).
 */
package com.carspa.carservice.service;

import com.carspa.carservice.dto.CarDto;
import com.carspa.carservice.model.Vehicle;
import com.carspa.carservice.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public List<CarDto.VehicleResponse> getMyVehicles(Long userId) {
        return vehicleRepository.findByUserIdAndActiveTrue(userId)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public CarDto.VehicleResponse addVehicle(CarDto.VehicleRequest request, Long userId) {
        // prevent duplicate plates for the same user
        if (vehicleRepository.existsByUserIdAndVehicleNumber(
                userId, request.getVehicleNumber().toUpperCase())) {
            throw new IllegalStateException(
                "Vehicle " + request.getVehicleNumber() + " is already registered to your account"
            );
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setUserId(userId);
        vehicle.setVehicleNumber(request.getVehicleNumber().toUpperCase());
        vehicle.setVehicleType(request.getVehicleType().toUpperCase());
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setColor(request.getColor());

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle {} added for user {}", saved.getVehicleNumber(), userId);
        return toResponse(saved);
    }

    @Transactional
    public CarDto.VehicleResponse updateVehicle(Long vehicleId, CarDto.VehicleRequest request, Long userId) {
        Vehicle vehicle = vehicleRepository.findByIdAndUserId(vehicleId, userId)
            .orElseThrow(() -> new RuntimeException("Vehicle not found or access denied"));

        vehicle.setVehicleType(request.getVehicleType().toUpperCase());
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setColor(request.getColor());

        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void deleteVehicle(Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleRepository.findByIdAndUserId(vehicleId, userId)
            .orElseThrow(() -> new RuntimeException("Vehicle not found or access denied"));
        // soft delete
        vehicle.setActive(false);
        vehicleRepository.save(vehicle);
        log.info("Vehicle {} soft-deleted by user {}", vehicleId, userId);
    }

    // ── private ──

    private CarDto.VehicleResponse toResponse(Vehicle v) {
        return CarDto.VehicleResponse.builder()
            .id(v.getId())
            .userId(v.getUserId())
            .vehicleNumber(v.getVehicleNumber())
            .vehicleType(v.getVehicleType())
            .brand(v.getBrand())
            .model(v.getModel())
            .color(v.getColor())
            .active(v.isActive())
            .createdAt(v.getCreatedAt())
            .build();
    }
}
