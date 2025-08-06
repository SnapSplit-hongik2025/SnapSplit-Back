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
@Table(name = "pay")
public class Pay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pay_id")
    private Long id;

    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "payer_id", nullable = false)
    private Long payerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", insertable = false, updatable = false)
    private TripMember payer;

    @Column(name = "pay_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal payAmount;

    @Column(name = "pay_amount_krw", nullable = false, precision = 10, scale = 2)
    private BigDecimal payAmountKrw;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false)
    private MemberType memberType;

    public enum MemberType {
        USER, SHARED_FUND
    }
}
