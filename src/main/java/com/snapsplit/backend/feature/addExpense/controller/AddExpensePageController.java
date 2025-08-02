package com.snapsplit.backend.feature.addExpense.controller;

import com.snapsplit.backend.feature.addExpense.dto.AddExpensePageResponse;
import com.snapsplit.backend.feature.addExpense.service.AddExpensePageService;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;


@RestController
@RequestMapping("/trips/{tripId}/expense")
@RequiredArgsConstructor
public class AddExpensePageController {

    private final AddExpensePageService addExpensePageService;
    @CheckTripMember
    @Operation(
            summary = "지출 추가 페이지 데이터 조회",
            description = "지출 추가를 위한 초기 데이터를 불러옵니다. 해당 여행(tripId)과 날짜(date)에 따라 통화, 환율, 멤버 정보 등을 포함합니다."
    )
    @GetMapping("/new")
    public ApiResponse<AddExpensePageResponse> getAddExpensePageData(
            @PathVariable Long tripId,
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ){

        AddExpensePageResponse response = addExpensePageService.getAddExpensePageData(tripId, date);
        return ApiResponse.success("지출 추가용 초기 데이터를 불러왔습니다.", response);
    }
}
