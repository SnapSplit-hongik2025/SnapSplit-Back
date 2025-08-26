package com.snapsplit.backend.domain.photo.repository;

import com.snapsplit.backend.domain.photo.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findAllByIdInAndAlbum_Trip_Id(List<Long> ids, Long tripId);
}