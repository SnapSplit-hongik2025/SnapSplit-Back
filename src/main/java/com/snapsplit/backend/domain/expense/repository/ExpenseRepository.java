package com.snapsplit.backend.domain.expense.repository;

import com.snapsplit.backend.domain.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByTripId(Long tripId);

}
