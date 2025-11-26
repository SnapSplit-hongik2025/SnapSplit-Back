package com.snapsplit.backend.feature.receipt.controller;

import com.snapsplit.backend.domain.totalshared.entity.TotalShared;
import com.snapsplit.backend.domain.totalshared.repository.TotalSharedRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.feature.receipt.dto.ReceiptRequest;
import com.snapsplit.backend.feature.receipt.dto.ReceiptResponse;
import com.snapsplit.backend.feature.receipt.service.ReceiptService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;

import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "영수증 OCR", description = "Document AI Expense Parser를 이용한 영수증 파싱")
@RestController
@RequiredArgsConstructor
@RequestMapping("/trips/{tripId}/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final TripRepository tripRepository;
    private final TotalSharedRepository totalSharedRepository;

    @Operation(summary = "영수증 파싱", description = "영수증 이미지를 업로드하면 영수증을 파싱하여 필요한 데이터만을 반환합니다.")
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReceiptResponse> parse(
            @PathVariable Long tripId,
            @ModelAttribute @Valid ReceiptRequest request
    ) {

        // Trip 조회
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 trip이 존재하지 않습니다."));

        // OCR 파싱
        ReceiptResponse data = receiptService.parse(request);

        // 여행에서 허용된 통화 목록 조회
        Set<String> allowedCurrencies = totalSharedRepository.findByTrip(trip).stream()
                .map(TotalShared::getTotalSharedCurrency)
                .collect(Collectors.toSet());

        // 영수증 통화 검증
        if (data.getCurrency() != null && !allowedCurrencies.contains(data.getCurrency())) {
            throw new IllegalArgumentException("여행에서 사용하지 않는 통화입니다: " + data.getCurrency());

        }

        return ApiResponse.success("영수증 파싱 성공", data);
    }

    // 업로드 관련 예외 간단 처리
    @ExceptionHandler(MultipartException.class)
    public ApiResponse<Object> handleMultipart(MultipartException e) {
        return ApiResponse.fail(400, "업로드 오류: " + e.getMessage());
    }
}
