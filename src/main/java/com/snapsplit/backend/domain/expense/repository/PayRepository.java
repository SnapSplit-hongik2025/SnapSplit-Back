package com.snapsplit.backend.domain.expense.repository;

import com.snapsplit.backend.domain.expense.entity.Pay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayRepository extends JpaRepository<Pay, Long> {

    @Modifying
    @Query("DELETE FROM Pay p WHERE p.expenseId = :expenseId")
    void deleteByExpenseId(@Param("expenseId") Long expenseId);

}
