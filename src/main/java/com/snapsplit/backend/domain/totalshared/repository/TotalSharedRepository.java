package com.snapsplit.backend.domain.totalshared.repository;

import com.snapsplit.backend.domain.totalshared.entity.TotalShared;
import com.snapsplit.backend.domain.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TotalSharedRepository extends JpaRepository<TotalShared, Long> {

    // trip_id와 currency가 일치하는 TotalShared 찾기
    Optional<TotalShared> findByTripAndTotalSharedCurrency(Trip trip, String totalSharedCurrency);
}
