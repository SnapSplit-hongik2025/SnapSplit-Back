package com.snapsplit.backend.domain.expense.repository;

import com.snapsplit.backend.domain.expense.entity.Split;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SplitRepository extends JpaRepository<Split, Long> {
    @Modifying
    @Query("DELETE FROM Split s WHERE s.expenseId = :expenseId")
    void deleteByExpenseId(@Param("expenseId") Long expenseId);

}
