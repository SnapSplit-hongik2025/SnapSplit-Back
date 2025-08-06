package com.snapsplit.backend.feature.settlement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SettlementPageResponse {
    private TripInfo trip; // 여행 정보
    private List<SettlementSummary> completeSettlement; // 정산 요약 리스트

    @Data
    @Builder
    public static class TripInfo {
        private LocalDate startDate; // 여행 시작일
        private LocalDate endDate;   // 여행 종료일
    }

    @Data
    @Builder
    public static class SettlementSummary {
        private Long id;             // 정산 아이디
        private LocalDate startDate; // 정산 시작일
        private LocalDate endDate;   // 정산 종료일
    }
}