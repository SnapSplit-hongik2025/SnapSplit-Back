package com.snapsplit.backend.domain.expense.repository;

import com.snapsplit.backend.domain.expense.entity.Split;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SplitRepository extends JpaRepository<Split, Long> {

    @Query("SELECT s FROM Split s WHERE s.expenseId = :expenseId")
    List<Split> findByExpenseId(@Param("expenseId") Long expenseId);
    List<Split> findByExpenseIdIn(List<Long> expenseIds);

    @Modifying
    @Query("DELETE FROM Split s WHERE s.expenseId = :expenseId")
    void deleteByExpenseId(@Param("expenseId") Long expenseId);

}
