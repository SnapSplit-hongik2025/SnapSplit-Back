package com.snapsplit.backend.feature.editTrip.controller;

import com.snapsplit.backend.feature.editTrip.dto.*;
import com.snapsplit.backend.feature.editTrip.service.EditTripService;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "여행 수정", description = "수정 전 정보 불러오기/수정하기/삭제하기")
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

    @CheckTripMember
    @Operation(summary = "여행 이름 및 대표 이미지 조회", description = "여행 제목과 대표 이미지를 조회합니다.")
    @GetMapping("/{tripId}/info")
    public ResponseEntity<ApiResponse<TripInfoResponse>> getTripInfo(@PathVariable Long tripId) {
        TripInfoResponse response = editTripService.getTripInfo(tripId);
        return ResponseEntity.ok(ApiResponse.success("여행 정보 조회 성공", response));
    }

    @CheckTripMember
    @Operation(summary = "여행 이름 및 대표 이미지 수정", description = "여행의 제목과 대표 이미지를 수정합니다.")
    @PatchMapping(value = "/{tripId}/info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateTripInfo(
            @PathVariable Long tripId,
            @RequestPart(value = "tripName", required = false) String tripName,
            @RequestPart(value = "tripImageFile", required = false) MultipartFile tripImageFile
    ) throws IOException {
        editTripService.updateTripInfo(tripId, tripName, tripImageFile);
        return ResponseEntity.ok(ApiResponse.success("여행 정보 수정 성공", null));
    }

    @CheckTripMember
    @Operation(summary = "여행 삭제", description = "해당 여행과 관련된 모든 데이터를 삭제합니다.")
    @DeleteMapping("/{tripId}")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(@PathVariable Long tripId) {
        editTripService.deleteTrip(tripId);
        return ResponseEntity.ok(ApiResponse.success("여행 삭제 성공", null));
    }


}