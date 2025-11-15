/**
 * CarServiceTest.java — unit tests for VehicleService and WashCentreService.
 * No Spring context, DB, or Eureka needed.
 */
package com.carspa.carservice.service;

import com.carspa.carservice.dto.CarDto;
import com.carspa.carservice.model.Vehicle;
import com.carspa.carservice.model.WashCentre;
import com.carspa.carservice.repository.VehicleRepository;
import com.carspa.carservice.repository.WashCentreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    // ── Vehicle tests ──

    @Mock VehicleRepository  vehicleRepository;
    @InjectMocks VehicleService vehicleService;

    @Mock WashCentreRepository washCentreRepository;
    @InjectMocks WashCentreService washCentreService;

    private Vehicle makeVehicle(Long id, Long userId) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setUserId(userId);
        v.setVehicleNumber("MH12AB1234");
        v.setVehicleType("SEDAN");
        v.setBrand("Maruti");
        v.setModel("Swift");
        v.setColor("White");
        v.setActive(true);
        return v;
    }

    private CarDto.VehicleRequest makeVehicleRequest() {
        CarDto.VehicleRequest req = new CarDto.VehicleRequest();
        req.setVehicleNumber("MH12AB1234");
        req.setVehicleType("SEDAN");
        req.setBrand("Maruti");
        req.setModel("Swift");
        req.setColor("White");
        return req;
    }

    @Test
    void addVehicle_newPlate_success() {
        when(vehicleRepository.existsByUserIdAndVehicleNumber(1L, "MH12AB1234")).thenReturn(false);
        when(vehicleRepository.save(any())).thenReturn(makeVehicle(1L, 1L));

        CarDto.VehicleResponse response = vehicleService.addVehicle(makeVehicleRequest(), 1L);

        assertThat(response.getVehicleNumber()).isEqualTo("MH12AB1234");
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void addVehicle_duplicatePlate_throwsConflict() {
        when(vehicleRepository.existsByUserIdAndVehicleNumber(1L, "MH12AB1234")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.addVehicle(makeVehicleRequest(), 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already registered");

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void deleteVehicle_wrongUser_throwsNotFound() {
        when(vehicleRepository.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.deleteVehicle(1L, 99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void getMyVehicles_returnsOnlyActiveVehicles() {
        when(vehicleRepository.findByUserIdAndActiveTrue(1L))
            .thenReturn(List.of(makeVehicle(1L, 1L)));

        List<CarDto.VehicleResponse> results = vehicleService.getMyVehicles(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isActive()).isTrue();
    }

    // ── Wash centre tests ──

    private WashCentre makeCentre(Long id, String name) {
        WashCentre wc = new WashCentre();
        wc.setId(id);
        wc.setName(name);
        wc.setAddress("MG Road");
        wc.setCity("Pune");
        wc.setPriceBasic(new BigDecimal("299.00"));
        wc.setPricePremium(new BigDecimal("499.00"));
        wc.setPriceFullDetail(new BigDecimal("899.00"));
        wc.setActive(true);
        return wc;
    }

    @Test
    void createCentre_newName_success() {
        CarDto.WashCentreRequest req = new CarDto.WashCentreRequest();
        req.setName("Pune Central");
        req.setAddress("MG Road");
        req.setCity("Pune");

        when(washCentreRepository.existsByNameIgnoreCase("Pune Central")).thenReturn(false);
        when(washCentreRepository.save(any())).thenReturn(makeCentre(1L, "Pune Central"));

        CarDto.WashCentreResponse response = washCentreService.createCentre(req);

        assertThat(response.getName()).isEqualTo("Pune Central");
    }

    @Test
    void createCentre_duplicateName_throwsConflict() {
        CarDto.WashCentreRequest req = new CarDto.WashCentreRequest();
        req.setName("Pune Central");
        req.setAddress("MG Road");
        req.setCity("Pune");

        when(washCentreRepository.existsByNameIgnoreCase("Pune Central")).thenReturn(true);

        assertThatThrownBy(() -> washCentreService.createCentre(req))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void getCentreById_notFound_throwsRuntimeException() {
        when(washCentreRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> washCentreService.getCentreById(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not found");
    }
}
