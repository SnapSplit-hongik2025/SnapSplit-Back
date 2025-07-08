package com.snapsplit.backend.domain.shared.repository;

import com.snapsplit.backend.domain.shared.entity.Shared;
import com.snapsplit.backend.domain.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharedRepository extends JpaRepository<Shared, Long> {

}
