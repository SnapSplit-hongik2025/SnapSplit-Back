package com.snapsplit.backend.domain.receipt.repository;

import com.snapsplit.backend.domain.receipt.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    Optional<Receipt> findByExpense_Id(Long expenseId);
}