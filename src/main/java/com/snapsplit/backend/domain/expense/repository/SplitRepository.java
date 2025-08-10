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

    // 여러 개의 expenseId에 대해 Split 검색
    List<Split> findAllByExpenseIdIn(List<Long> expenseIds);

    // splitterId와 여러 expenseId에 대한 split 내역 찾기
    List<Split> findAllByExpenseIdInAndSplitterId(List<Long> expenseIds, Long splitterId);
}
