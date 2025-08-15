package com.snapsplit.backend.domain.album.repository;

import com.snapsplit.backend.domain.album.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    Optional<Album> findByTripId(Long tripId);
}