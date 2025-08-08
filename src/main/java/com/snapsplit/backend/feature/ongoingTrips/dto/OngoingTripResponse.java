package com.snapsplit.backend.feature.ongoingTrips.dto;

import com.snapsplit.backend.domain.trip.entity.Trip;
import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class OngoingTripResponse {

    private Long tripId;
    private String tripName;
    private String startDate;
    private String endDate;
    private String tripImage;
    private List<String> countryNames;

    public static OngoingTripResponse from(Trip trip, List<String> countryNames) {
        return OngoingTripResponse.builder()
                .tripId(trip.getId())
                .tripName(trip.getTripName())
                .startDate(trip.getStartDate().toString())
                .endDate(trip.getEndDate().toString())
                .tripImage(trip.getTripImage())
                .countryNames(countryNames)
                .build();
    }
}
