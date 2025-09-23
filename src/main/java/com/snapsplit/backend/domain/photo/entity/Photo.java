package com.snapsplit.backend.domain.photo.entity;

import com.snapsplit.backend.domain.album.entity.Album;
import com.snapsplit.backend.domain.phototag.entity.PhotoTag;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "photo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Long id;

    // Photo는 하나의 Album에 속합니다. (N:1 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @Column(name = "s3_url", nullable = false)
    private String s3Url;

    @Column(name = "photo_dt")
    private LocalDateTime photoDt;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // Photo는 여러 개의 PhotoTag를 가질 수 있습니다. (1:N 관계)
    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhotoTag> photoTags = new ArrayList<>();

    @Builder
    public Photo(Album album, String s3Url, LocalDateTime photoDt, Double latitude, Double longitude) {
        this.album = album;
        this.s3Url = s3Url;
        this.photoDt = photoDt;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getObjectKey() {
        // https://...amazonaws.com/ 뒤 문자열만 추출
        int idx = s3Url.indexOf(".amazonaws.com/");
        if (idx == -1) {
            throw new IllegalStateException("Invalid S3 URL format: " + s3Url);
        }
        return s3Url.substring(idx + ".amazonaws.com/".length());
    }

    public String getFileName() {
        return s3Url.substring(s3Url.lastIndexOf("/") + 1);
    }

}