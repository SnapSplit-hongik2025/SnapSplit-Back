package com.snapsplit.backend.feature.getSharedDetails.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateDefaultCurrencyResponse {
    private String before; // 변경 전 대표 통화코드
    private String after; // 변경 후 대표 통화코드
}
