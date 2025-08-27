package com.snapsplit.backend.domain.phototag.repository;

import com.snapsplit.backend.domain.photo.entity.Photo;
import com.snapsplit.backend.domain.phototag.entity.PhotoTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoTagRepository extends JpaRepository<PhotoTag, Long> {
    // 특정 사진들에 해당하는 모든 태그를 한 번에 삭제
    void deleteAllByPhotoIn(List<Photo> photos);

    // 특정 사진(photoId)에 연결된 모든 태그를 삭제
    void deleteAllByPhotoId(Long photoId);
}