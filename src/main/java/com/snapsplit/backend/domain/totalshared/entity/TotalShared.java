package com.snapsplit.backend.domain.totalshared.entity;

import com.snapsplit.backend.domain.trip.entity.Trip;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "total_shared")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TotalShared {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "total_shared_id")
    private Long id; // 경비총액 아이디

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip; // 여행 아이디

    @Column(name = "total_shared_amount", nullable = false)
    private BigDecimal totalSharedAmount; // 총 경비

    @Column(name = "total_shared_currency", nullable = false, length = 10)
    private String totalSharedCurrency; // 통화

    @Column(name = "latest_modified", nullable = false)
    private LocalDate latestModified; // 마지막 수정일
}
