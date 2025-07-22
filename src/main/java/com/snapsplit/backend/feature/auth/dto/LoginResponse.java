package com.snapsplit.backend.feature.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String name;
    private String profileImage;
    private String userCode;
}
