package com.snapsplit.backend.feature.createTrip.controller;

import org.springframework.web.bind.annotation.RequestBody;
import com.snapsplit.backend.feature.createTrip.dto.CreateTripRequest;
import com.snapsplit.backend.feature.createTrip.dto.TripResponse;
import com.snapsplit.backend.feature.createTrip.service.CreateTripService;
import com.snapsplit.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class CreateTripController {

    private final CreateTripService createTripService;

    // 신규 여행 등록하기
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<List<TripResponse>>> createTrip(@RequestBody CreateTripRequest request) {

        // 여행 생성 후 여행 아이디 리턴받기
        Long tripId = createTripService.createTrip(request);
        // 현재 시각 받아오기
        String now = LocalDateTime.now().toString();

        TripResponse response = new TripResponse(tripId, now);
        List<TripResponse> data = List.of(response);

        return ResponseEntity.ok(
                ApiResponse.success("여행이 성공적으로 등록되었습니다.", data)
        );
    }
}
