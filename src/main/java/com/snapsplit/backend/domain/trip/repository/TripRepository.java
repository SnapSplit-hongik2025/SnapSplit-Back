package com.snapsplit.backend.domain.trip.repository;

import com.snapsplit.backend.domain.trip.entity.Trip;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {
    @Query("""
    select tm.trip
    from TripMember tm
    where tm.user.id = :userId
      and tm.trip.startDate > :today
    order by tm.trip.startDate asc
""")
    List<Trip> findUpcomingTripsByUserId(@Param("userId") Long userId, @Param("today") LocalDate today);
}
