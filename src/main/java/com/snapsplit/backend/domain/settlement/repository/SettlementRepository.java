package com.snapsplit.backend.domain.settlement.repository;

import com.snapsplit.backend.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findAllByTripId(Long tripId);

    // trip.id가 tripId이고, startDate <= date <= endDate 인 데이터 존재 여부
    boolean existsByTrip_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long tripId, LocalDate startDate, LocalDate endDate
    );

    //
    @Query("""
        select (count(s) > 0)
        from Settlement s
        where s.trip.id = :tripId
          and s.startDate <= :endDate
          and s.endDate >= :startDate
    """)
    boolean existsOverlapping(
            @Param("tripId") Long tripId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query("DELETE FROM Settlement s WHERE s.trip.id = :tripId")
    void deleteByTripId(@Param("tripId") Long tripId);

}
