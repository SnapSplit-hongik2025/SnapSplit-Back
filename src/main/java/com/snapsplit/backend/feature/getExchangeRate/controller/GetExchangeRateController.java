package com.snapsplit.backend.feature.getExchangeRate.controller;

import com.snapsplit.backend.feature.getExchangeRate.dto.ExchangeRateResponse;
import com.snapsplit.backend.feature.getExchangeRate.service.ExchangeRateService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GetExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "환율 조회", description = "원하는 국가의 현재 환율을 조회합니다.")
    @GetMapping("/exchangeRate")
    public ApiResponse<ExchangeRateResponse> getExchangeRate(@RequestParam String base) {
        ExchangeRateResponse result = exchangeRateService.fetchExchangeRate(base);
        return ApiResponse.success("환율 조회 성공", result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return ApiResponse.fail(400, e.getMessage());
    }

}
