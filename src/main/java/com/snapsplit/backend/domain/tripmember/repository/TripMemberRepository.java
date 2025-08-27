package com.snapsplit.backend.domain.tripmember.repository;

import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.tripmember.entity.MemberType;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import org.springframework.data.domain.Pageable;
import com.snapsplit.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TripMemberRepository extends JpaRepository<TripMember, Long> {

    // userId에 해당하는 사용자가 참여 중인 Trip 중에서,
    // 오늘 이후 시작하는 여행들을 startDate 오름차순으로 조회
    @Query("""
    select tm.trip
    from TripMember tm
    where tm.user.id = :userId
      and tm.trip.startDate > :today
    order by tm.trip.startDate asc
    """)
    List<Trip> findUpcomingTripsByUserId(@Param("userId") Long userId, @Param("today") LocalDate today);

    // userId에 해당하는 사용자가 참여한 Trip 중에서,
    // endDate가 오늘보다 이전인 Trip들을 최신 순으로 limit만큼 가져오는 쿼리
    @Query("""
    select tm.trip
    from TripMember tm
    where tm.user.id = :userId
      and tm.trip.endDate < :today
    order by tm.trip.endDate desc
""")
    List<Trip> findPastTripsByUserId(@Param("userId") Long userId, @Param("today") LocalDate today, Pageable pageable);

    // userId가 해당하는 사용자가 참여한 Trip 중에서,
    // startDate가 오늘보다 이전이고, endDate가 오늘보다 이후인 Trip 가져오기
    @Query("""
    select tm.trip
    from TripMember tm
    where tm.user.id = :userId
      and tm.trip.startDate <= :today
      and tm.trip.endDate >= :today
    order by tm.trip.startDate asc
""")
    List<Trip> findOngoingTripsByUserId(@Param("userId") Long userId, @Param("today") LocalDate today);

    List<TripMember> findAllByTripId(Long tripId);
    
    boolean existsByTripAndUser(Trip trip, User user); // 이미 여행에 참여한 사용자인지 확인

    @Query("SELECT tm FROM TripMember tm JOIN FETCH tm.user WHERE tm.trip.id = :tripId")
    List<TripMember> findAllByTripIdWithUser(@Param("tripId") Long tripId);

    int countByTrip_IdAndMemberType(Long tripId, MemberType memberType);
    List<TripMember> findAllByIdInAndTrip_Id(List<Long> ids, Long tripId);
}