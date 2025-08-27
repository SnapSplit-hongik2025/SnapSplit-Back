package com.snapsplit.backend.domain.expense.entity;

import com.snapsplit.backend.domain.receipt.entity.Receipt;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Long id;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "expense_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal expenseAmount;

    @Column(name = "expense_currency", length = 10, nullable = false)
    private String expenseCurrency;

    @Column(name = "expense_krw", nullable = false, precision = 10, scale = 2)
    private BigDecimal expenseKrw;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    @Column(name = "expense_name", length = 50)
    private String expenseName;

    @Column(name = "expense_memo", columnDefinition = "TEXT")
    private String expenseMemo;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @OneToOne(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private Receipt receipt;

    public enum Category {
        FLIGHT, ACCOMMODATION, FOOD, TRANSPORTATION, TOUR, SHOPPING, OTHERS
    }

    public enum PaymentMethod {
        CASH, CREDIT_CARD
    }
}
