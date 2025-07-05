package com.snapsplit.backend.feature.getCountryTrip.service;

import com.snapsplit.backend.domain.country.entity.Country;
import com.snapsplit.backend.domain.country.repository.CountryRepository;
import com.snapsplit.backend.feature.getCountryTrip.dto.CountryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CountryListService {

    private final CountryRepository countryRepository;

    // 전체 국가 목록 조회
    public List<CountryListResponse> getAllCountries() {
        List<Country> countries = countryRepository.findAll();
        return countries.stream()
                .map(country -> CountryListResponse.builder()
                        .countryId(country.getId())
                        .countryName(country.getCountryName())
                        .build())
                .collect(Collectors.toList());
    }
}
