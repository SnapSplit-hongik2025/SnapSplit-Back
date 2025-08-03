package com.snapsplit.backend.feature.tripHome.controller;

import com.snapsplit.backend.feature.tripHome.dto.TripHomeResponse;
import com.snapsplit.backend.feature.tripHome.service.TripHomeService;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripHomeController {

    private final TripHomeService tripHomeService;

    @GetMapping("/{tripId}/expenses")
    @CheckTripMember
    public ApiResponse<TripHomeResponse> getTripHome(@PathVariable Long tripId) {
        TripHomeResponse response = tripHomeService.getTripHome(tripId);
        return ApiResponse.success("여행 상세 조회 성공", response);
    }
}
