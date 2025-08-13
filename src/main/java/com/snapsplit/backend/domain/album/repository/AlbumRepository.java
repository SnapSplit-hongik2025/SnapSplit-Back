package com.snapsplit.backend.domain.album.repository;

import com.snapsplit.backend.domain.album.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {
}