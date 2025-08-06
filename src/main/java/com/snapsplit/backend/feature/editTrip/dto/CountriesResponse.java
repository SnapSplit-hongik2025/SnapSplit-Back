package com.snapsplit.backend.feature.editTrip.dto;

import java.util.List;

public record CountriesResponse(
        List<CountryDto> countries,
        List<CountryDto> selectedCountries
) {
    public record CountryDto(
            Long countryId,
            String countryName
    ) {}
}