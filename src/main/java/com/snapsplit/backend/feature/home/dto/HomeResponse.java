package com.snapsplit.backend.feature.home.dto;

import com.snapsplit.backend.feature.ongoingTrips.dto.OngoingTripResponse;
import com.snapsplit.backend.feature.pastTrips.dto.PastTripResponse;
import com.snapsplit.backend.feature.upcomingTrips.dto.UpcomingTripResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HomeResponse {

    private List<UpcomingTripResponse> upcomingTrips; // 다가오는 여행
    private List<OngoingTripResponse> ongoingTrips; // 진행중인 여행
    private List<PastTripResponse> pastTrips; // 이전 여행

}
