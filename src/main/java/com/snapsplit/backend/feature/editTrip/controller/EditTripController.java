package com.snapsplit.backend.feature.editTrip.controller;

import com.snapsplit.backend.feature.editTrip.dto.CountriesResponse;
import com.snapsplit.backend.feature.editTrip.dto.UpdateCountriesRequest;
import com.snapsplit.backend.feature.editTrip.service.EditTripService;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trips")
public class EditTripController {

    private final EditTripService editTripService;
    @CheckTripMember
    @Operation(summary = "여행 국가 조회", description = "기존에 등록된 여행 국가 목록을 불러옵니다.")
    @GetMapping("/{tripId}/countries")
    public ResponseEntity<ApiResponse<CountriesResponse>> getTripCountries(@PathVariable Long tripId) {
        CountriesResponse response = editTripService.getTripCountries(tripId);
        return ResponseEntity.ok(ApiResponse.success("여행 국가 조회 성공", response));
    }

    @CheckTripMember
    @Operation(summary = "여행 국가 수정", description = "기존에 등록된 여행 국가 목록을 수정합니다.")
    @PatchMapping("/{tripId}/countries")
    public ResponseEntity<ApiResponse> updateTripCountries(
            @PathVariable Long tripId,
            @RequestBody UpdateCountriesRequest request
    ) {
        editTripService.updateCountries(tripId, request);
        return ResponseEntity.ok(ApiResponse.<Void>success("여행 국가 수정 성공", null));
    }

}