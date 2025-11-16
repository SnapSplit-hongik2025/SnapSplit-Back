package com.snapsplit.backend.feature.pastTrips.controller;

import com.snapsplit.backend.feature.pastTrips.dto.PastTripResponse;
import com.snapsplit.backend.feature.pastTrips.dto.PastTripsResponse;
import com.snapsplit.backend.feature.pastTrips.service.PastTripsService;
import com.snapsplit.backend.global.response.ApiResponse;
import com.snapsplit.backend.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "홈", description = "기본 홈/코드로 여행 참여/다가오는 여행/지난 여행")
@RestController
@RequiredArgsConstructor
@RequestMapping("/trips")
public class PastTripsController {

    private final PastTripsService pastTripsService;

    @Operation(summary = "과거 여행 목록 조회", description = "사용자가 참여한 과거 여행 목록을 조회합니다.")
    @GetMapping("/past")

    public ResponseEntity<ApiResponse<PastTripsResponse>> getPastTrips(
            @AuthenticationPrincipal CustomUserPrincipal user) {
        PastTripsResponse response = pastTripsService.getPastTripsWithStats(user.getId());

        return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noStore())   // 캐시 금지
                .body(ApiResponse.success("지난 여행 조회 성공", response));
    }

}
