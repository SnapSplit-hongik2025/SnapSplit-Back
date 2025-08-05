package com.snapsplit.backend.feature.editTrip.controller;

import com.snapsplit.backend.feature.editTrip.dto.CountriesResponse;
import com.snapsplit.backend.feature.editTrip.dto.ScheduleResponse;
import com.snapsplit.backend.feature.editTrip.dto.UpdateCountriesRequest;
import com.snapsplit.backend.feature.editTrip.dto.UpdateScheduleRequest;
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

    @CheckTripMember
    @Operation(summary = "여행 일정 조회", description = "기존 여행의 시작일과 종료일을 조회합니다.")
    @GetMapping("/{tripId}/schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getTripSchedule(
            @PathVariable Long tripId
    ) {
        ScheduleResponse response = editTripService.getTripSchedule(tripId);
        return ResponseEntity.ok(ApiResponse.success("여행 일정 조회 성공", response));
    }

    @CheckTripMember
    @Operation(summary = "여행 일정 수정", description = "여행의 시작일과 종료일을 수정합니다.")
    @PatchMapping("/{tripId}/schedule")
    public ResponseEntity<ApiResponse<Void>> updateTripSchedule(
            @PathVariable Long tripId,
            @RequestBody UpdateScheduleRequest request
    ) {
        editTripService.updateTripSchedule(tripId, request);
        return ResponseEntity.ok(ApiResponse.success("여행 일정 수정 성공",null));
    }


}