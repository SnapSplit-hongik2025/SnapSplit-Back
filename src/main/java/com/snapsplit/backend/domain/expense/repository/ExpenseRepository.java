package com.snapsplit.backend.domain.expense.repository;

import com.snapsplit.backend.domain.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByTripId(Long tripId);

    // 여행 ID + 날짜 범위 조건으로 Expense 조회
    List<Expense> findByTripIdAndExpenseDateBetween(Long tripId, LocalDate startDate, LocalDate endDate);
}
