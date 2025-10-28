package com.snapsplit.backend.domain.photo.repository;

import com.snapsplit.backend.domain.photo.entity.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findAllByIdInAndAlbum_Trip_Id(List<Long> ids, Long tripId);
    Optional<Photo> findByIdAndAlbum_Trip_Id(Long id, Long tripId);
    Slice<Photo> findByAlbum_Id(Long albumId, Pageable pageable);

    // 태그된 사진 조회: @Query 사용 + @Param 이름 일치
    @Query("""
        SELECT DISTINCT p
        FROM Photo p
        JOIN p.photoTags pt
        WHERE p.album.id = :albumId
          AND pt.user.id = :memberId
    """)
    Slice<Photo> findTaggedPhotosByAlbumAndUser(@Param("albumId") Long albumId,
                                                @Param("memberId") Long memberId,
                                                Pageable pageable);
}