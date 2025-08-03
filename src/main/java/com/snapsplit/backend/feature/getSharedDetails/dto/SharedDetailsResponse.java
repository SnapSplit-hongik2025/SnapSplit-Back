package com.snapsplit.backend.feature.getSharedDetails.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SharedDetailsResponse {
    private Long tripId; // 여행 아이디
    private String tripStartDate; // 여행 시작일
    private String defaultCurrency; // 대표 통화
    private List<SharedDayGroup> sharedBudgetDetails; // 공동경비 세부내역
    private List<CurrencyAmount> totalSharedBudget; // 이용가능 통화 및 공동경비 잔액

    @Data
    @Builder
    public static class SharedDayGroup {
        private String date;
        private List<SharedItem> items;
    }

    @Data
    @Builder
    public static class SharedItem {
        private String type; // deposit or withdraw or expense
        private String title; // 지출명
        private String memo; // expense일 때만 지출내용 제공
        private BigDecimal amount; // 지출액
        private BigDecimal amountKRW; // 한화 지출액

    }

    @Data
    @Builder
    public static class CurrencyAmount {
        private String currency; // 통화
        private BigDecimal amount; // 공동경비 잔액
    }
}
