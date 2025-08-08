package com.snapsplit.backend.feature.updateTotalShared.controller;

import com.snapsplit.backend.feature.updateTotalShared.dto.UpdateTotalSharedPageResponse;
import com.snapsplit.backend.feature.updateTotalShared.service.UpdateTotalSharedPageService;

import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공동경비", description = "공동경비 조회/넣기/빼기/대표 통화 변경")
@RestController
@RequiredArgsConstructor
@RequestMapping("/trips/{tripId}/budget")
public class UpdateTotalSharedPageController {

    private final UpdateTotalSharedPageService updateTotalSharedPageService;

    @Operation(summary = "공동경비 수정 페이지 로드", description = "공동경비 추가 및 삭제에서 필요한 정보를 제공합니다.")
    @GetMapping
    @CheckTripMember
    public ResponseEntity<ApiResponse<UpdateTotalSharedPageResponse>> getUpdateTotalSharedPage(
            @PathVariable Long tripId
    ) {
        UpdateTotalSharedPageResponse response = updateTotalSharedPageService.getPageData(tripId);
        return ResponseEntity.ok(ApiResponse.success("공동경비 수정 페이지 로드 성공", response));
    }
}
