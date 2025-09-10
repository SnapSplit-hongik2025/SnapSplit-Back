package com.snapsplit.backend.feature.pastTrips.dto;

import com.snapsplit.backend.domain.trip.entity.Trip;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class PastTripResponse {

    private Long tripId;
    private String tripName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String tripImage;
    private List<String> countries;

    public static PastTripResponse from(Trip trip, List<String> countries) {
        return new PastTripResponse(
                trip.getId(),
                trip.getTripName(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getTripImage(),
                countries
        );
    }
}