package com.snapsplit.backend.feature.addExpense.dto;

import java.math.BigDecimal;
import java.util.List;

public record AddExpenseRequest(
        ExpenseDto expense,
        List<PayerDto> payers,
        List<SplitterDto> splitters
) {

    public record ExpenseDto(
            Integer day,
            BigDecimal amount,
            String currency,
            BigDecimal exchangeRate,
            String category,
            String expense_name,
            String expense_memo,
            String paymentMethod
    ) {}

    public record PayerDto(
            Long tripMemberId,
            BigDecimal payerAmount
    ) {}

    public record SplitterDto(
            Long tripMemberId,
            BigDecimal splitAmount
    ) {}
}
