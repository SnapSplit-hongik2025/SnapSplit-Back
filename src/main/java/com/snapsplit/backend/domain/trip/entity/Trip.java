package com.snapsplit.backend.domain.trip.entity;

import com.snapsplit.backend.domain.tripcountry.entity.TripCountry;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trip")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_id")
    private Long id; // 여행 아이디

    @Column(name = "trip_name", length = 100, nullable = false)
    private String tripName; // 여행 이름

    @Column(nullable = false)
    private LocalDate startDate; // 시작일

    @Column(nullable = false)
    private LocalDate endDate; // 종료일

    @Column(name = "trip_image", length = 255)
    private String tripImage; // 여행 대표사진

    @Column(name = "trip_total_expense")
    private BigDecimal tripTotalExpense; // 지출 총액

    // 여행 - 여행국가 1 : n 관계
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripCountry> tripCountries = new ArrayList<>();
}
