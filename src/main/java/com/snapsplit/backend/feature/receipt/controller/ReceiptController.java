package com.snapsplit.backend.feature.receipt.controller;

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

@Tag(name = "영수증 OCR", description = "Document AI Expense Parser를 이용한 영수증 파싱")
@RestController
@RequiredArgsConstructor
@RequestMapping("/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;

    @Operation(summary = "영수증 파싱", description = "영수증 이미지를 업로드하면 영수증을 파싱하여 필요한 데이터만을 반환합니다.")
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReceiptResponse> parse(@ModelAttribute @Valid ReceiptRequest request) {
        ReceiptResponse data = receiptService.parse(request);
        return ApiResponse.success("영수증 파싱 성공", data);
    }

    // 업로드 관련 예외 간단 처리
    @ExceptionHandler(MultipartException.class)
    public ApiResponse<Object> handleMultipart(MultipartException e) {
        return ApiResponse.fail(400, "업로드 오류: " + e.getMessage());
    }
}
