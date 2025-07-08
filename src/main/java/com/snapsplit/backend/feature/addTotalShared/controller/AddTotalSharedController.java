package com.snapsplit.backend.feature.addTotalShared.controller;

import com.snapsplit.backend.feature.addTotalShared.dto.AddTotalSharedRequest;
import com.snapsplit.backend.feature.addTotalShared.dto.AddTotalSharedResponse;
import com.snapsplit.backend.feature.addTotalShared.service.AddTotalSharedService;
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
public class AddTotalSharedController {

    private final AddTotalSharedService addTotalSharedService;

    // 공동 경비 추가하기
    @Operation(summary = "공동 경비 추가", description = "공동 경비를 추가합니다.")
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<AddTotalSharedResponse>> addTotalShared(
            @PathVariable Long tripId,
            @RequestBody AddTotalSharedRequest request) {

        try {
            AddTotalSharedResponse response = addTotalSharedService.addTotalShared(tripId, request);
            return ResponseEntity.ok(ApiResponse.success("공동경비가 성공적으로 추가되었습니다.", response));
        } catch (IllegalArgumentException e) {
            // trip_id에 해당하는 여행이 없는 경우
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.fail(400, e.getMessage()));
        }
    }
}