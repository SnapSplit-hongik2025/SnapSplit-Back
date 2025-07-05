package com.snapsplit.backend.feature.getCountryTrip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryListResponse {
    private Long countryId; // 국가 아이디
    private String countryName; // 국가명
}
