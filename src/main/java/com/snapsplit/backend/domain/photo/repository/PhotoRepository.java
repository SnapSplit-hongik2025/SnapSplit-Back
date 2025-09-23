package com.snapsplit.backend.domain.photo.repository;

import com.snapsplit.backend.domain.photo.entity.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findAllByIdInAndAlbum_Trip_Id(List<Long> ids, Long tripId);
    Optional<Photo> findByIdAndAlbum_Trip_Id(Long id, Long tripId);
    Page<Photo> findByAlbum_Id(Long albumId, Pageable pageable);

    // 특정 앨범에서, 특정 사용자가 태그된 사진들만 페이지네이션으로 조회
    @Query("SELECT p FROM Photo p JOIN p.photoTags pt WHERE p.album.id = :albumId AND pt.user.id = :memberId")
    Page<Photo> findTaggedPhotosByAlbumAndUser(@Param("albumId") Long albumId, @Param("memberId") Long memberId, Pageable pageable);
}