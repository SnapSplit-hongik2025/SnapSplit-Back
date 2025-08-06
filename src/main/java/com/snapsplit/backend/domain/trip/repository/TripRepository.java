package com.snapsplit.backend.domain.trip.repository;

import com.snapsplit.backend.domain.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findByTripCode(String tripCode); // TripCode로 Trip 찾기

    @Modifying
    @Query("UPDATE Trip t SET t.defaultCurrency = :defaultCurrency WHERE t.id = :tripId")
    void updateDefaultCurrencyById(@Param("tripId") Long tripId, @Param("defaultCurrency") String defaultCurrency);
}
