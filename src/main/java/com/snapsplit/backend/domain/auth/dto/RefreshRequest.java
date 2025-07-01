package com.snapsplit.backend.domain.auth.dto;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}
