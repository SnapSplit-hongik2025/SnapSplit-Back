package com.snapsplit.backend.feature.editTrip.dto;

import lombok.Builder;

import java.util.List;

public record UpdateCountriesRequest(
        List<CountryDto> countries
) {
    @Builder
    public record CountryDto(
            Long countryId,
            String countryName
    ) {}
}