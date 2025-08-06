package com.snapsplit.backend.feature.createTrip.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class CreateTripRequest {
    private String tripName; // 여행 이름
    private List<CountryDto> countries; // 여행 국가 리스트
    private String startDate; // 여행 시작일
    private String endDate; // 여행 종료일
    private List<Long> membersId; // 여행 멤버 리스트
}
