package com.snapsplit.backend.domain.expense.repository;

import com.snapsplit.backend.domain.expense.entity.Pay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PayRepository extends JpaRepository<Pay, Long> {

    @Query("SELECT p FROM Pay p WHERE p.expenseId = :expenseId")
    List<Pay> findByExpenseId(@Param("expenseId") Long expenseId);

    @Modifying
    @Query("DELETE FROM Pay p WHERE p.expenseId = :expenseId")
    void deleteByExpenseId(@Param("expenseId") Long expenseId);

}
