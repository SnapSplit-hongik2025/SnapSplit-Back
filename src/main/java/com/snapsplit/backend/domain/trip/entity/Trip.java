package com.snapsplit.backend.domain.trip.entity;

import com.snapsplit.backend.domain.shared.entity.Shared;
import com.snapsplit.backend.domain.totalshared.entity.TotalShared;
import com.snapsplit.backend.domain.tripcountry.entity.TripCountry;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
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

    @Column(name = "trip_code", length = 20, nullable = false, unique = true)
    private String tripCode; // 여행 참여 코드

    @Column(name = "default_currency", length = 20, nullable = false)
    private String defaultCurrency; // 대표 통화

    // 여행 - 여행국가 1 : n 관계
    // 여행이 삭제되면 TripCountry도 삭제되도록
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripCountry> tripCountries = new ArrayList<>();

    // 여행 - 여행 멤버 1 : n 관계
    // 여행이 삭제되면 TripMember도 삭제되도록
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripMember> tripMembers = new ArrayList<>();

    // 여행 - 경비 총액 1 : n 관계
    // 여행이 삭제되면 TotalShared도 삭제되도록
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TotalShared> totalSharedList = new ArrayList<>();

    // 여행 - 경비 1 : n 관계
    // 여행이 삭제되면 Shared도 삭제되도록
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Shared> sharedList = new ArrayList<>();
}
