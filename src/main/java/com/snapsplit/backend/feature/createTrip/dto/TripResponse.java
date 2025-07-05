package com.snapsplit.backend.feature.createTrip.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TripResponse {
    private Long tripId; // 여행 아이디
    private String createdAt; // 생성일자
}
