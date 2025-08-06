package com.snapsplit.backend.feature.getExchangeRate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ExchangeRateResponse {
    private String date; // 기준일
    private List<ExchangeRateItem> rates; // 여러 통화 환율 리스트

    @Getter
    @Builder
    public static class ExchangeRateItem {
        private String code; // 통화 코드 (ex. USD, EUR)
        private BigDecimal rateToBase; // 기준 통화(KRW) 대비 환율
    }
}
