package com.snapsplit.backend.feature.getCategoryExpense.dto;

import com.snapsplit.backend.domain.expense.entity.Expense;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

public record CategoryExpenseResponse(
        BigDecimal totalAmountKRW,
        List<CategoryExpenseDto> categoryExpenses
) {

    @Builder
    public record CategoryExpenseDto(
            Expense.Category category,
            BigDecimal amountKRW
    ) {}
}
