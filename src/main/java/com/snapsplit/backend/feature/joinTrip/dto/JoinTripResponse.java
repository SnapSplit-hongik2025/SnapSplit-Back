package com.snapsplit.backend.feature.joinTrip.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class JoinTripResponse {
    private Long tripId; // 참여한 여행 아이디
}