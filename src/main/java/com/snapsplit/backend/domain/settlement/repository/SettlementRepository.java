package com.snapsplit.backend.domain.settlement.repository;

import com.snapsplit.backend.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findAllByTripId(Long tripId);
}
