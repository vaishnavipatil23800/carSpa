/**
 * WashCentreController.java — wash centre catalogue endpoints.
 *
 * Public (user) endpoints:
 *   GET /api/cars/centres           — all active centres
 *   GET /api/cars/centres/{id}      — single centre details
 *   GET /api/cars/centres/city/{c}  — filter by city
 *
 * Admin endpoints:
 *   POST   /api/cars/admin/centres       — create centre
 *   PUT    /api/cars/admin/centres/{id}  — update centre
 *   DELETE /api/cars/admin/centres/{id}  — deactivate centre
 *
 * Note: admin role enforcement is done at the gateway level via route config.
 * The controller trusts that if the request arrived here, the gateway
 * already validated the JWT and the X-User-Role header says ROLE_ADMIN.
 */
package com.carspa.carservice.controller;

import com.carspa.carservice.dto.CarDto;
import com.carspa.carservice.service.WashCentreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
@Tag(name = "Wash Centres", description = "Wash centre catalogue — browse and manage locations")
public class WashCentreController {

    private final WashCentreService washCentreService;

    // ── User (read-only) ──

    @GetMapping("/centres")
    @Operation(summary = "List all active wash centres")
    public ResponseEntity<List<CarDto.WashCentreResponse>> getAllCentres() {
        return ResponseEntity.ok(washCentreService.getAllActiveCentres());
    }

    @GetMapping("/centres/{id}")
    @Operation(summary = "Get a single wash centre by ID")
    public ResponseEntity<CarDto.WashCentreResponse> getCentreById(@PathVariable Long id) {
        return ResponseEntity.ok(washCentreService.getCentreById(id));
    }

    @GetMapping("/centres/city/{city}")
    @Operation(summary = "Filter wash centres by city")
    public ResponseEntity<List<CarDto.WashCentreResponse>> getCentresByCity(
        @PathVariable String city
    ) {
        return ResponseEntity.ok(washCentreService.getCentresByCity(city));
    }

    // ── Admin (write) ──

    @PostMapping("/admin/centres")
    @Operation(summary = "Admin — create a new wash centre")
    public ResponseEntity<CarDto.WashCentreResponse> createCentre(
        @Valid @RequestBody CarDto.WashCentreRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(washCentreService.createCentre(request));
    }

    @PutMapping("/admin/centres/{id}")
    @Operation(summary = "Admin — update wash centre details")
    public ResponseEntity<CarDto.WashCentreResponse> updateCentre(
        @PathVariable               Long id,
        @Valid @RequestBody CarDto.WashCentreRequest request
    ) {
        return ResponseEntity.ok(washCentreService.updateCentre(id, request));
    }

    @DeleteMapping("/admin/centres/{id}")
    @Operation(summary = "Admin — deactivate a wash centre")
    public ResponseEntity<Void> deactivateCentre(@PathVariable Long id) {
        washCentreService.deactivateCentre(id);
        return ResponseEntity.noContent().build();
    }
}
