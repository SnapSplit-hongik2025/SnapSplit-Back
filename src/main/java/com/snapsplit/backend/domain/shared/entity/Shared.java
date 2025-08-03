package com.snapsplit.backend.domain.shared.entity;

import com.snapsplit.backend.domain.trip.entity.Trip;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "shared")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Shared {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shared_id")
    private Long id; // 경비 아이디

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip; // 여행 아이디

    @Column(name = "shared_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount; // 경비 입금액

    @Column(name = "shared_amount_krw", nullable = false)
    private BigDecimal amountKRW; // 한화 환산 금액

    @Column(name = "shared_currency", length = 10, nullable = false)
    private String currency; // 경비 입금 통화

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt; // 경비 입금일자

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod; // 경비 입금 방식

    @Enumerated(EnumType.STRING)
    @Column(name = "shared_type", nullable = false)
    private SharedType sharedType; // 사용 유형 (입금/출금/지출)

    @Column(name = "expense_id")
    private Long expenseId; // 지출에 의해 차감된 경우 연관된 지출 ID (nullable)
}
