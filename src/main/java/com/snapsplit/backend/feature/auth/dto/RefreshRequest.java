package com.snapsplit.backend.feature.auth.dto;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}
