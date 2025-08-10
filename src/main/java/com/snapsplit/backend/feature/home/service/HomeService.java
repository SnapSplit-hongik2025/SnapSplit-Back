package com.snapsplit.backend.feature.home.service;

import com.snapsplit.backend.feature.home.dto.HomeResponse;
import com.snapsplit.backend.feature.ongoingTrips.dto.OngoingTripResponse;
import com.snapsplit.backend.feature.ongoingTrips.service.OngoingTripsService;
import com.snapsplit.backend.feature.pastTrips.service.PastTripsService;
import com.snapsplit.backend.feature.upcomingTrips.service.UpcomingTripsService;
import com.snapsplit.backend.feature.pastTrips.dto.PastTripResponse;
import com.snapsplit.backend.feature.upcomingTrips.dto.UpcomingTripResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final PastTripsService pastTripsService;
    private final UpcomingTripsService upcomingTripsService;
    private final OngoingTripsService ongoingTripsService;

    // 기본 홈 정보 조회하기
    public HomeResponse getHome(Long userId) {
        // 지난 여행: limit 5
        List<PastTripResponse> pastTrips = pastTripsService.getPastTripsPreview(userId, 5);
        List<UpcomingTripResponse> upcomingTrips = upcomingTripsService.getUpcomingTrips(userId);
        List<OngoingTripResponse> ongoingTrips = ongoingTripsService.getOngoingTrips(userId);

        return HomeResponse.builder()
                .upcomingTrips(upcomingTrips)
                .ongoingTrips(ongoingTrips)
                .pastTrips(pastTrips)
                .build();
    }
}
