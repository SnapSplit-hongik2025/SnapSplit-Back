package com.snapsplit.backend.feature.upcomingTrips.controller;

import com.snapsplit.backend.feature.upcomingTrips.dto.UpcomingTripResponse;
import com.snapsplit.backend.feature.upcomingTrips.service.UpcomingTripsService;
import com.snapsplit.backend.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trips")
public class UpcomingTripsController {

    private final UpcomingTripsService upcomingTripsService;

    @Operation(summary = "다가오는 여행 목록 조회", description = "현재 날짜 기준으로 사용자의 다가오는 여행 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "성공적으로 조회됨")
    @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @GetMapping("/upcoming")
    public ResponseEntity<List<UpcomingTripResponse>> getUpcomingTrips(
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<UpcomingTripResponse> trips = upcomingTripsService.getUpcomingTrips(user.getId());
        return ResponseEntity.ok(trips);
    }
}
