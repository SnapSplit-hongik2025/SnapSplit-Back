package com.snapsplit.backend.feature.getTripCode.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetTripCodeResponse {
    private String tripCode;
}
