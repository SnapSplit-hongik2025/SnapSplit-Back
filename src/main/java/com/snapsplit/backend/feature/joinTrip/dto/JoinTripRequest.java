package com.snapsplit.backend.feature.joinTrip.dto;

import lombok.Data;

@Data
public class JoinTripRequest {
    private Long userId; // 유저 아이디
    private String inviteCode; // 초대 코드
}