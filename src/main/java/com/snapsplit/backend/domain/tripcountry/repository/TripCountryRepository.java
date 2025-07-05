package com.snapsplit.backend.domain.tripcountry.repository;

import com.snapsplit.backend.domain.tripcountry.entity.TripCountry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripCountryRepository extends JpaRepository<TripCountry, Long> {
}
