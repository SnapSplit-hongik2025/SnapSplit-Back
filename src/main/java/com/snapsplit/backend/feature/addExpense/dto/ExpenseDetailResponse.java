package com.snapsplit.backend.feature.addExpense.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

@Builder
public record ExpenseDetailResponse(
        Long expenseId,
        BigDecimal amount,
        BigDecimal amountKRW,
        String currency,
        String paymentMethod,
        LocalDate date,
        boolean canAddExpense,
        String expenseName,
        String expenseMemo,
        String category,
        List<MemberAmountDto> payers,
        List<MemberAmountDto> splitters,
        // 영수증으로 지출 추가한 경우
        String receiptUrl,
        List<ReceiptItemDto> receiptItems
) {
    @Builder
    public record MemberAmountDto(
            Long memberId,
            String name,
            BigDecimal amount
    ) {}

    @Builder
    public record ReceiptItemDto(
            String name,
            BigDecimal amount
    ) {}
}
