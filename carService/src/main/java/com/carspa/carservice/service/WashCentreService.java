/**
 * WashCentreService.java — wash centre catalogue management.
 *
 * Read operations are cached with Caffeine (expires after 60s).
 * Write operations (admin only) evict the cache so users see fresh data.
 *
 * The wash centre list is seeded with some Pune locations
 * via data.sql on first run so the frontend isn't empty.
 */
package com.carspa.carservice.service;

import com.carspa.carservice.dto.CarDto;
import com.carspa.carservice.model.WashCentre;
import com.carspa.carservice.repository.WashCentreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WashCentreService {

    private final WashCentreRepository washCentreRepository;

    // ── Public: read ──

    @Cacheable("washCentres")
    public List<CarDto.WashCentreResponse> getAllActiveCentres() {
        log.debug("Loading wash centres from DB (cache miss)");
        return washCentreRepository.findByActiveTrueOrderByNameAsc()
            .stream().map(this::toResponse).toList();
    }

    @Cacheable(value = "washCentresByCity", key = "#city.toLowerCase()")
    public List<CarDto.WashCentreResponse> getCentresByCity(String city) {
        return washCentreRepository.findByCityIgnoreCaseAndActiveTrue(city)
            .stream().map(this::toResponse).toList();
    }

    public CarDto.WashCentreResponse getCentreById(Long id) {
        return toResponse(findById(id));
    }

    // ── Admin: write ──

    @Transactional
    @CacheEvict(value = {"washCentres", "washCentresByCity"}, allEntries = true)
    public CarDto.WashCentreResponse createCentre(CarDto.WashCentreRequest request) {
        if (washCentreRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalStateException("A wash centre named '" + request.getName() + "' already exists");
        }

        WashCentre centre = mapFromRequest(new WashCentre(), request);
        WashCentre saved  = washCentreRepository.save(centre);
        log.info("Wash centre '{}' created", saved.getName());
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = {"washCentres", "washCentresByCity"}, allEntries = true)
    public CarDto.WashCentreResponse updateCentre(Long id, CarDto.WashCentreRequest request) {
        WashCentre centre = findById(id);
        mapFromRequest(centre, request);
        return toResponse(washCentreRepository.save(centre));
    }

    @Transactional
    @CacheEvict(value = {"washCentres", "washCentresByCity"}, allEntries = true)
    public void deactivateCentre(Long id) {
        WashCentre centre = findById(id);
        centre.setActive(false);
        washCentreRepository.save(centre);
        log.info("Wash centre '{}' deactivated", centre.getName());
    }

    // ── private helpers ──

    private WashCentre findById(Long id) {
        return washCentreRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Wash centre not found: #" + id));
    }

    private WashCentre mapFromRequest(WashCentre centre, CarDto.WashCentreRequest req) {
        centre.setName(req.getName());
        centre.setAddress(req.getAddress());
        centre.setCity(req.getCity());
        centre.setPincode(req.getPincode());
        centre.setPhone(req.getPhone());
        centre.setOperatingHours(req.getOperatingHours());
        centre.setPriceBasic(req.getPriceBasic());
        centre.setPricePremium(req.getPricePremium());
        centre.setPriceFullDetail(req.getPriceFullDetail());
        if (req.getCapacity() != null) centre.setCapacity(req.getCapacity());
        return centre;
    }

    private CarDto.WashCentreResponse toResponse(WashCentre w) {
        return CarDto.WashCentreResponse.builder()
            .id(w.getId())
            .name(w.getName())
            .address(w.getAddress())
            .city(w.getCity())
            .pincode(w.getPincode())
            .phone(w.getPhone())
            .operatingHours(w.getOperatingHours())
            .priceBasic(w.getPriceBasic())
            .pricePremium(w.getPricePremium())
            .priceFullDetail(w.getPriceFullDetail())
            .rating(w.getRating())
            .capacity(w.getCapacity())
            .active(w.isActive())
            .build();
    }
}
