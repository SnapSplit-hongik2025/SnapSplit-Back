package com.snapsplit.backend.feature.addTotalShared.controller;

import com.snapsplit.backend.feature.addTotalShared.dto.AddTotalSharedRequest;
import com.snapsplit.backend.feature.addTotalShared.dto.AddTotalSharedResponse;
import com.snapsplit.backend.feature.addTotalShared.service.RemoveTotalSharedService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/trips/{tripId}/budget")
@RequiredArgsConstructor
public class RemoveTotalSharedController {

    private final RemoveTotalSharedService removeTotalSharedService;

    // 공동 경비 빼기
    @Operation(summary = "공동 경비 빼기", description = "공동 경비를 회수합니다.")
    @PostMapping("/remove")
    public ResponseEntity<ApiResponse<AddTotalSharedResponse>> removeTotalShared(
            @PathVariable Long tripId,
            @RequestBody AddTotalSharedRequest request) {

        try { // 성공적으로 회수되었을 경우
            AddTotalSharedResponse response = removeTotalSharedService.removeTotalShared(tripId, request);
            return ResponseEntity.ok(ApiResponse.success("공동경비가 성공적으로 회수되었습니다.", response));
        } catch (IllegalArgumentException e) {
            // 해당 여행을 못 찾았거나
            // 여행과 통화가 일치하는 totalShared를 못 찾았거나
            // 회수 금액이 기존 totalShared보다 적을 경우
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.fail(400, e.getMessage()));
        }

    }
}
