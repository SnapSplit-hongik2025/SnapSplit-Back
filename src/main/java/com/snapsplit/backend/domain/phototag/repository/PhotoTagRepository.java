package com.snapsplit.backend.domain.phototag.repository;

import com.snapsplit.backend.domain.phototag.entity.PhotoTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoTagRepository extends JpaRepository<PhotoTag, Long> {
}