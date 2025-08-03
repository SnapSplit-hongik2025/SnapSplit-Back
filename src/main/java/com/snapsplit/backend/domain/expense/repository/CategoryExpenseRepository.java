package com.snapsplit.backend.domain.expense.repository;

import com.snapsplit.backend.domain.expense.entity.CategoryExpense;
import com.snapsplit.backend.domain.expense.entity.Expense;
import com.snapsplit.backend.domain.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryExpenseRepository extends JpaRepository<CategoryExpense, Long> {
    List<CategoryExpense> findByTrip(Trip trip);
    Optional<CategoryExpense> findByTripAndCategory(Trip trip, Expense.Category category);

}
