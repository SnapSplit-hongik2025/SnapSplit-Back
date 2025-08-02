package com.snapsplit.backend.feature.addExpense.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ExpenseDetailResponse(
        Long expenseId,
        BigDecimal amount,
        BigDecimal amountKRW,
        String currency,
        String paymentMethod,
        String date,
        String expenseName,
        String expenseMemo,
        String category,
        List<MemberAmountDto> payers,
        List<MemberAmountDto> splitters
) {
    @Builder
    public record MemberAmountDto(
            Long memberId,
            String name,
            BigDecimal amount
    ) {}
}
