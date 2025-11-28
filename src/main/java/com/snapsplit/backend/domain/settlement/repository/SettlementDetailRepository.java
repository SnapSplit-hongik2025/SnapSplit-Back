package com.snapsplit.backend.domain.settlement.repository;

import com.snapsplit.backend.domain.settlement.entity.SettlementDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SettlementDetailRepository extends JpaRepository<SettlementDetail, Long> {
    List<SettlementDetail> findAllBySettlementId(Long settlementId);
    @Modifying
    @Query("DELETE FROM SettlementDetail sd WHERE sd.settlement.trip.id = :tripId")
    void deleteByTripId(@Param("tripId") Long tripId);


}
