package com.snapsplit.backend.feature.updateTotalShared.controller;

import com.snapsplit.backend.feature.updateTotalShared.dto.AddTotalSharedRequest;
import com.snapsplit.backend.feature.updateTotalShared.dto.AddTotalSharedResponse;
import com.snapsplit.backend.feature.updateTotalShared.service.AddTotalSharedService;
import com.snapsplit.backend.global.response.ApiResponse;
import jakarta.persistence.OptimisticLockException;
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

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<AddTotalSharedResponse>> addTotalShared(
            @PathVariable Long tripId,
            @RequestBody AddTotalSharedRequest request) {

        try {
            AddTotalSharedResponse response = addTotalSharedService.addTotalShared(tripId, request);
            return ResponseEntity.ok(ApiResponse.success("공동경비가 성공적으로 추가되었습니다.", response));
        } catch (OptimisticLockException e) {
            return ResponseEntity
                    .status(409) // 409 Conflict
                    .body(ApiResponse.fail(409, "동시에 여러 요청이 처리되어 충돌이 발생했습니다. 다시 시도해주세요."));
        } catch (IllegalArgumentException e) {
            // trip_id에 해당하는 여행이 없는 경우
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.fail(400, e.getMessage()));
        }
    }
}