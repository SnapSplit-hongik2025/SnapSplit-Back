package com.snapsplit.backend.domain.tripcountry.repository;

import com.snapsplit.backend.domain.tripcountry.entity.TripCountry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripCountryRepository extends JpaRepository<TripCountry, Long> {
    //각 Trip에 해당하는 국가명 추출을 위해 TripCountry조회
    List<TripCountry> findAllByTripId(Long tripId);
}
