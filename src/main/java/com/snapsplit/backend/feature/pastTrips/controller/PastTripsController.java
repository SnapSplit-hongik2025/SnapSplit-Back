package com.snapsplit.backend.feature.pastTrips.controller;

import com.snapsplit.backend.feature.pastTrips.dto.PastTripResponse;
import com.snapsplit.backend.feature.pastTrips.service.PastTripsService;
import com.snapsplit.backend.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trips")
public class PastTripsController {

    private final PastTripsService pastTripsService;

    @Operation(summary = "과거 여행 목록 조회", description = "사용자가 참여한 과거 여행 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/past")
    public ResponseEntity<List<PastTripResponse>> getPastTrips(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @RequestParam(required = false) Integer limit
    ) {
        List<PastTripResponse> trips = pastTripsService.getPastTrips(user.getId(), limit);
        return ResponseEntity.ok(trips);
    }
}
