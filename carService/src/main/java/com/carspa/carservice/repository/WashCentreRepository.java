package com.carspa.carservice.repository;

import com.carspa.carservice.model.WashCentre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WashCentreRepository extends JpaRepository<WashCentre, Long> {

    // only return active centres to users
    List<WashCentre> findByActiveTrueOrderByNameAsc();

    // search by city
    List<WashCentre> findByCityIgnoreCaseAndActiveTrue(String city);

    boolean existsByNameIgnoreCase(String name);
}
