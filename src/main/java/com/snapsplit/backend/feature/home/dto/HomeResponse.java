package com.snapsplit.backend.feature.home.dto;

import com.snapsplit.backend.feature.pastTrips.dto.PastTripResponse;
import com.snapsplit.backend.feature.upcomingTrips.dto.UpcomingTripResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HomeResponse {

    private List<UpcomingTripResponse> upcomingTrips;
    private List<PastTripResponse> pastTrips;

}
