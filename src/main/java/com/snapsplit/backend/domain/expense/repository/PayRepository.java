package com.snapsplit.backend.domain.expense.repository;

import com.snapsplit.backend.domain.expense.entity.Pay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PayRepository extends JpaRepository<Pay, Long> {

    @Query("SELECT p FROM Pay p WHERE p.expenseId = :expenseId")
    List<Pay> findByExpenseId(@Param("expenseId") Long expenseId);

    @Modifying
    @Query("DELETE FROM Pay p WHERE p.expenseId = :expenseId")
    void deleteByExpenseId(@Param("expenseId") Long expenseId);

    // 여러 개의 expenseId에 대해서 Pay 검색
    List<Pay> findAllByExpenseIdIn(List<Long> expenseIds);

    // payId와 여러 expenseId에 대한 pay 내역 찾기
    List<Pay> findAllByExpenseIdInAndPayerId(List<Long> expenseIds, Long payerId);
}
