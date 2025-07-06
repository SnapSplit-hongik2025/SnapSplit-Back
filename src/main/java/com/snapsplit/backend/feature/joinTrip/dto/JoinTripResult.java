package com.snapsplit.backend.feature.joinTrip.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoinTripResult {
    private boolean success; // 성공 여부
    private String message; // 메세지
    private JoinTripResponse data; // 데이터
}
