package com.snapsplit.backend.domain.trip.repository;

import com.snapsplit.backend.domain.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {
}
