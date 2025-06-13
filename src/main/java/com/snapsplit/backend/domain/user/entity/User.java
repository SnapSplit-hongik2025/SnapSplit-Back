package com.snapsplit.backend.domain.user.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id") // ← ERD 기준
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(name = "kakao_id", length = 100, unique = true, nullable = false)
    private String kakaoId;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "user_code", length = 50, unique = true, nullable = false)
    private String userCode;
}
