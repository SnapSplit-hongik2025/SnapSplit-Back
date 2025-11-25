package com.snapsplit.backend.feature.tripHome.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record TripHomeResponse(
        Long tripId,
        String tripName,
        LocalDate startDate,
        LocalDate endDate,
        List<String> countries,
        List<String> memberProfileImages,
        SharedFundDto sharedFund,
        TopCategoryExpenseDto topCategoryExpense,
        List<DailyExpenseDto> dailyExpenses,
        BigDecimal totalExpense
) {

    @Builder
    public record SharedFundDto(
            String defaultCurrency,
            BigDecimal balance
    ) {}

    @Builder
    public record TopCategoryExpenseDto(
            String category,
            BigDecimal amountKRW
    ) {}

    @Builder
    public record DailyExpenseDto(
            LocalDate date,
            List<ExpenseDto> expenses,
            boolean canAddExpense
    ) {}

    @Builder
    public record ExpenseDto(
            Long expenseId,
            String category,
            String expenseName,
            String expenseMemo,
            BigDecimal amount,
            String currency,
            List<String> splitters // 이름만 담김
    ) {}
}
