package com.snapsplit.backend.feature.getExchangeRate.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExchangeRateResponse {
    private String base; // 국가
    private double rateToKrw; // 환율
    private String date; // 환율을 받아온 날
}
