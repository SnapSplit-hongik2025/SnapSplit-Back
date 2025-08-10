package com.snapsplit.backend.feature.addExpense.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Builder
public record AddExpensePageResponse(
        String defaultCurrency,
        List<String> availCurrencies,
        Map<String, BigDecimal> exchangeRates,
        String defaultDate,
        List<MemberDto> members,
        List<String> settledDates
) {

    @Builder
    public record MemberDto(
            Long memberId,
            String name,
            String memberType // USER | SHARED_FUND
    ) {}
}
