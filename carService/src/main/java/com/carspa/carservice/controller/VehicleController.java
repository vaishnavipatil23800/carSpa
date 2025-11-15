/**
 * VehicleController.java — user's vehicle management endpoints.
 *
 * User identity from X-User-Id header (gateway-injected — no JWT needed here).
 *
 * GET    /api/cars/vehicles           — my vehicles
 * POST   /api/cars/vehicles           — add a vehicle
 * PUT    /api/cars/vehicles/{id}      — update a vehicle
 * DELETE /api/cars/vehicles/{id}      — remove a vehicle (soft delete)
 */
package com.carspa.carservice.controller;

import com.carspa.carservice.dto.CarDto;
import com.carspa.carservice.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "User vehicle registration and management")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @Operation(summary = "Get all my registered vehicles")
    public ResponseEntity<List<CarDto.VehicleResponse>> getMyVehicles(
        @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(vehicleService.getMyVehicles(Long.parseLong(userId)));
    }

    @PostMapping
    @Operation(summary = "Register a new vehicle")
    public ResponseEntity<CarDto.VehicleResponse> addVehicle(
        @Valid @RequestBody          CarDto.VehicleRequest request,
        @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(vehicleService.addVehicle(request, Long.parseLong(userId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update vehicle details")
    public ResponseEntity<CarDto.VehicleResponse> updateVehicle(
        @PathVariable               Long   id,
        @Valid @RequestBody         CarDto.VehicleRequest request,
        @RequestHeader("X-User-Id") String userId
    ) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request, Long.parseLong(userId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a vehicle from my account")
    public ResponseEntity<Void> deleteVehicle(
        @PathVariable               Long   id,
        @RequestHeader("X-User-Id") String userId
    ) {
        vehicleService.deleteVehicle(id, Long.parseLong(userId));
        return ResponseEntity.noContent().build();
    }
}
