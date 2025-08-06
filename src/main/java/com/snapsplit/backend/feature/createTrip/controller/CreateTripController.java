package com.snapsplit.backend.feature.createTrip.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.snapsplit.backend.feature.createTrip.dto.CreateTripRequest;
import com.snapsplit.backend.feature.createTrip.dto.TripResponse;
import com.snapsplit.backend.feature.createTrip.service.CreateTripService;
import com.snapsplit.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class CreateTripController {

    private final CreateTripService createTripService;

    // 신규 여행 등록하기
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "신규 여행 등록하기", description = "새로운 여행을 등록합니다.")
    public ResponseEntity<ApiResponse<List<TripResponse>>> createTrip(
            @RequestPart("request") CreateTripRequest request,
            @RequestPart(value = "tripImage", required = false) MultipartFile tripImageFile
    ) throws IOException {
        Long tripId = createTripService.createTrip(request, tripImageFile);
        String now = LocalDateTime.now().toString();
        TripResponse response = new TripResponse(tripId, now);
        return ResponseEntity.ok(ApiResponse.success("여행이 성공적으로 등록되었습니다.", List.of(response)));
    }
}
