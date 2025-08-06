package com.snapsplit.backend.domain.expense.entity;

import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "split")
public class Split {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "split_id")
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "splitter_id", nullable = false)
    private Long splitterId;

    @Column(name = "split_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal splitAmount;

    @Column(name = "split_amount_krw", nullable = false, precision = 10, scale = 2)
    private BigDecimal splitAmountKrw;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "splitter_id", insertable = false, updatable = false)
    private TripMember splitter;
}

