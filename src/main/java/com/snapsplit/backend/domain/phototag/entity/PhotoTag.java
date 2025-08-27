package com.snapsplit.backend.domain.phototag.entity;

import com.snapsplit.backend.domain.photo.entity.Photo;
import com.snapsplit.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "photo_tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhotoTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_tag_id")
    private Long id;

    // PhotoTag는 하나의 Photo에 연결됩니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id", nullable = false)
    private Photo photo;

    // PhotoTag는 하나의 User에 연결됩니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 서비스 로직에서 new PhotoTag(photo, user) 형태로 쉽게 생성하기 위한 생성자
    public PhotoTag(Photo photo, User user) {
        this.photo = photo;
        this.user = user;
    }
}