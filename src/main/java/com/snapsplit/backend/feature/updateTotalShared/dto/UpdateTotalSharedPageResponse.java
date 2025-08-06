package com.snapsplit.backend.feature.updateTotalShared.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class UpdateTotalSharedPageResponse {

    private String defaultCurrency; // 대표 통화

    private List<CurrencyRate> currencies; // 환율 리스트

    @Data
    @Builder
    public static class CurrencyRate {
        private String code; // 통화 코드
        private BigDecimal exchangeRate; // 환율
    }
}
