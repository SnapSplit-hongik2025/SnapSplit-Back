package com.snapsplit.backend.domain.tripcountry.entity;

import com.snapsplit.backend.domain.country.entity.Country;
import com.snapsplit.backend.domain.trip.entity.Trip;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trip_country")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TripCountry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_country_id")
    private Long id; // 여행국가 아이디

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip; // 여행 아이디

    @ManyToOne
    @JoinColumn(name = "country_id", nullable = false)
    private Country country; // 국가 아이디
}
