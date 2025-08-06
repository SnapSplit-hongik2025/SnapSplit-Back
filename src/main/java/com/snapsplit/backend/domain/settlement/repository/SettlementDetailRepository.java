package com.snapsplit.backend.domain.settlement.repository;

import com.snapsplit.backend.domain.settlement.entity.SettlementDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementDetailRepository extends JpaRepository<SettlementDetail, Long> {
    List<SettlementDetail> findAllBySettlementId(Long settlementId);
}
