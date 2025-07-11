package com.snapsplit.backend.feature.getExchangeRate.controller;

import com.snapsplit.backend.feature.getExchangeRate.dto.ExchangeRateResponse;
import com.snapsplit.backend.feature.getExchangeRate.service.ExchangeRateService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GetExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "현재 일자의 환율 조회", description = "원하는 국가의 현재 환율을 조회합니다.")
    @GetMapping("/exchangeRate")
    public ApiResponse<ExchangeRateResponse> getExchangeRate(@RequestParam String base) {
        ExchangeRateResponse result = exchangeRateService.fetchExchangeRate(base);
        return ApiResponse.success("환율 조회 성공", result);
    }

}
