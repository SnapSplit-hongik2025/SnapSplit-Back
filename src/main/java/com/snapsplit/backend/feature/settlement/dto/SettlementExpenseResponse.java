package com.snapsplit.backend.feature.settlement.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SettlementExpenseResponse {

    private List<SettlementDetailByMember> settlementDetailsByMember;
    private BigDecimal totalKRW; // 총 지출액

    @Data
    @Builder
    public static class SettlementDetailByMember {
        private String date; // 날짜
        private List<ExpenseItem> items; // 지출 상세내역
    }

    @Data
    @Builder
    public static class ExpenseItem {
        private String expenseName; // 지출명
        private String expenseMemo; // 지출상세
        private BigDecimal amount; // 지출액
        private BigDecimal amountKRW; // 한화 지출액
        private String expenseCurrency; // 통화
    }
}
