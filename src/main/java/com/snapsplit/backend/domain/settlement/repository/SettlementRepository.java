package com.snapsplit.backend.domain.settlement.repository;

import com.snapsplit.backend.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findAllByTripId(Long tripId);

    // trip.id가 tripId이고, startDate <= date <= endDate 인 데이터 존재 여부
    boolean existsByTrip_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long tripId, LocalDate startDate, LocalDate endDate
    );
}
