package com.snapsplit.backend.domain.shared.repository;

import com.snapsplit.backend.domain.shared.entity.Shared;
import com.snapsplit.backend.domain.shared.entity.SharedType;
import com.snapsplit.backend.domain.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SharedRepository extends JpaRepository<Shared, Long> {

    @Modifying
    @Query("DELETE FROM Shared s WHERE s.trip.id = :tripId AND s.expenseId = :expenseId AND s.sharedType = :sharedType")
    void deleteByTripIdAndExpenseIdAndSharedType(
            @Param("tripId") Long tripId,
            @Param("expenseId") Long expenseId,
            @Param("sharedType") SharedType sharedType
    );

    // 여행과 통화로 Shared 검색
    List<Shared> findByTrip(Trip trip);

    // 통화로 필터링
    List<Shared> findByTripAndCurrency(Trip trip, String sharedCurrency);
}
