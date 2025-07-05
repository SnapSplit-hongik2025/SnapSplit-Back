package com.snapsplit.backend.feature.getCountryTrip.controller;

import com.snapsplit.backend.feature.getCountryTrip.dto.CountryListResponse;
import com.snapsplit.backend.feature.getCountryTrip.service.CountryListService;
import com.snapsplit.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/countries")
@RequiredArgsConstructor
public class CountryListController {

    private final CountryListService countryListService;

    @GetMapping
    public ApiResponse<List<CountryListResponse>> getAllCountries() {
        List<CountryListResponse> result = countryListService.getAllCountries();
        return ApiResponse.success("국가 리스트 조회 성공", result);
    }
}
