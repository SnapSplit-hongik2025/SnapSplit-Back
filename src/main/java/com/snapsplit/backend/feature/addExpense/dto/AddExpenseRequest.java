package com.snapsplit.backend.feature.addExpense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record AddExpenseRequest(
        @NotNull @Valid
        ExpenseDto expense,
        @NotNull @Size(min=1)
        List<PayerDto> payers,
        @NotNull @Size(min=1)
        List<SplitterDto> splitters
) {

    public record ExpenseDto(
            @NotNull
            LocalDate date,
            @NotNull @DecimalMin(value = "0.0", inclusive = false)
            BigDecimal amount,
            @NotBlank
            String currency,
            @NotNull @DecimalMin(value = "0.0", inclusive = false)
            BigDecimal exchangeRate,
            @NotBlank
            String category,
            String expenseName,
            String expenseMemo,
            @NotBlank
            String paymentMethod
    ) {}

    public record PayerDto(
            @NotNull
            Long memberId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false)
            BigDecimal payerAmount
    ) {}

    public record SplitterDto(
            @NotNull
            Long memberId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false)
            BigDecimal splitAmount
    ) {}
}
