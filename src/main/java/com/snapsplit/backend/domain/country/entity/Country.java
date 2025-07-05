package com.snapsplit.backend.domain.country.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "country")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "country_id")
    private Long id; // 국가 아이디

    @Column(name = "country_name", length = 100, nullable = false)
    private String countryName; // 국가 이름

    @Column(length = 20)
    private String currency; // 통화

    @Column(name = "country_image", length = 255)
    private String countryImage; // 국가 대표 이미지
}
