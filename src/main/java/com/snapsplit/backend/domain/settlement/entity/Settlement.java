package com.snapsplit.backend.domain.settlement.entity;

import com.snapsplit.backend.domain.trip.entity.Trip;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "settlement")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 정산 아이디

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip; // 여행 아이디

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt; // 정산 생성일자

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // 정산 시작일

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // 정산 종료일

    // 정산 세부내역 1 : N 관계
    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementDetail> details = new ArrayList<>();
}
