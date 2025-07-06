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

    @Column(name = "shared_currency", length = 10, nullable = false)
    private String currency; // 경비 입금 통화

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt; // 경비 입금일자

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod; // 경비 입금 방식

}
