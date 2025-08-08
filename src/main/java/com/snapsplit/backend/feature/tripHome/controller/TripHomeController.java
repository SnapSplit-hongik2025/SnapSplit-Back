package com.snapsplit.backend.feature.tripHome.controller;

import com.snapsplit.backend.feature.tripHome.dto.TripHomeResponse;
import com.snapsplit.backend.feature.tripHome.service.TripHomeService;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "개별 지출", description = "지출 추가/수정/삭제/조회")
@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripHomeController {

    private final TripHomeService tripHomeService;

    @Operation(
            summary = "여행 홈 정보 조회, 개별 지출의 목록",
            description = "여행 기본 정보, 공동 경비 잔액, 누적 지출 가장 큰 카테고리 정보, 날짜별 지출 내역, 총합을 포함한 홈 화면 데이터를 반환합니다."
                    + " 참고: date 쿼리 파라미터(?date=YYYY-MM-DD)는 프론트엔드 스크롤 위치 조정용으로만 사용되며, 백엔드에서는 무시됩니다."
    )
    @GetMapping("/{tripId}/expenses")
    @CheckTripMember
    public ApiResponse<TripHomeResponse> getTripHome(@PathVariable Long tripId) {
        TripHomeResponse response = tripHomeService.getTripHome(tripId);
        return ApiResponse.success("여행 상세 조회 성공", response);
    }
}
