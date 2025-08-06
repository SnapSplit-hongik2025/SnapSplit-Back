package com.snapsplit.backend.feature.getExchangeRate.controller;

import com.snapsplit.backend.feature.getExchangeRate.dto.ExchangeRateResponse;
import com.snapsplit.backend.feature.getExchangeRate.service.ExchangeRateService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GetExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "환율 조회", description = "원하는 통화 코드 리스트에 대한 환율을 조회합니다.")
    @GetMapping("/exchangeRate")
    public ApiResponse<ExchangeRateResponse> getExchangeRates(@RequestParam List<String> bases) {
        ExchangeRateResponse result = exchangeRateService.fetchExchangeRate(bases);
        return ApiResponse.success("환율 조회 성공", result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return ApiResponse.fail(400, e.getMessage());
    }

}
