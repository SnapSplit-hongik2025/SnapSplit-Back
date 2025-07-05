package com.snapsplit.backend.domain.tripmember.repository;

import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripMemberRepository extends JpaRepository<TripMember, Long> {
}
