package com.snapsplit.backend.domain.album.entity;

import com.snapsplit.backend.domain.photo.entity.Photo;
import com.snapsplit.backend.domain.trip.entity.Trip;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "album")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "album_id")
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    // Album은 하나의 Trip에 속합니다. (1:1 관계)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    // Album은 여러 개의 Photo를 가질 수 있습니다. (1:N 관계)
    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Photo> photos = new ArrayList<>();

    @Builder
    public Album(String name, Trip trip) {
        this.name = name;
        this.trip = trip;
    }
}