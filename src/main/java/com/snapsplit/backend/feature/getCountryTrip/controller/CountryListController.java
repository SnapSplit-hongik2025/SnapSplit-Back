package com.snapsplit.backend.feature.getCountryTrip.controller;

import com.snapsplit.backend.feature.getCountryTrip.dto.CountryListResponse;
import com.snapsplit.backend.feature.getCountryTrip.service.CountryListService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "여행 생성", description = "국가 목록/사용자 검색/신규 여행 등록")
@RestController
@RequestMapping("/countries")
@RequiredArgsConstructor
public class CountryListController {

    private final CountryListService countryListService;

    // 전체 국가 목록 조회
    @Operation(summary = "전체 국가 목록 조회", description = "여행에 등록 가능한 전체 국가 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<CountryListResponse>> getAllCountries() {
        List<CountryListResponse> result = countryListService.getAllCountries();
        return ApiResponse.success("국가 리스트 조회 성공", result);
    }
}
