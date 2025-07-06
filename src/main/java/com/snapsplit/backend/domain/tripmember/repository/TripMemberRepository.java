package com.snapsplit.backend.domain.tripmember.repository;

import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TripMemberRepository extends JpaRepository<TripMember, Long> {

    // userId에 해당하는 사용자가 참여 중인 여행 중,
    // 오늘 이후 시작하는 여행들을 startDate 오름차순으로 조회
    @Query("""
    select tm.trip
    from TripMember tm
    where tm.user.id = :userId
      and tm.trip.startDate > :today
    order by tm.trip.startDate asc
    """)
    List<Trip> findUpcomingTripsByUserId(@Param("userId") Long userId, @Param("today") LocalDate today);

}