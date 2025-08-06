package com.snapsplit.backend.feature.settlement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SettlementRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}
