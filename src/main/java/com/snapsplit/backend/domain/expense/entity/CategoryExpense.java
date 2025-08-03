package com.snapsplit.backend.domain.expense.entity;

import com.snapsplit.backend.domain.trip.entity.Trip;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "category_expense",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "category"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CategoryExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_expense_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private Expense.Category category;  // 💡 여기만 이렇게 수정

    @Column(name = "amount_krw", nullable = false)
    private BigDecimal amountKRW;

    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void increase(BigDecimal amount) {
        this.amountKRW = this.amountKRW.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void decrease(BigDecimal amount) {
        this.amountKRW = this.amountKRW.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
