package com.snapsplit.backend.feature.editTrip.service;
import com.snapsplit.backend.domain.country.entity.Country;
import com.snapsplit.backend.domain.country.repository.CountryRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.tripcountry.entity.TripCountry;
import com.snapsplit.backend.feature.editTrip.dto.CountriesResponse;
import com.snapsplit.backend.feature.editTrip.dto.ScheduleResponse;
import com.snapsplit.backend.feature.editTrip.dto.UpdateCountriesRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EditTripService {

    private final TripRepository tripRepository;
    private final CountryRepository countryRepository;

    // 수정 전 여행지 불러오기
    @Transactional(readOnly = true)
    public CountriesResponse getTripCountries(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 여행이 존재하지 않습니다."));

        List<CountriesResponse.CountryDto> countryDtos = trip.getTripCountries().stream()
                .map(tripCountry -> new CountriesResponse.CountryDto(
                        tripCountry.getCountry().getId(),
                        tripCountry.getCountry().getCountryName()
                ))
                .toList();

        return new CountriesResponse(countryDtos);
    }

    // 여행지 수정하기
    @Transactional
    public void updateCountries(Long tripId, UpdateCountriesRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 여행이 존재하지 않습니다."));

        // 기존 여행지 목록 초기화 (orphanRemoval = true 덕분에 자동 삭제)
        trip.getTripCountries().clear();

        // 새로운 국가 목록 설정
        List<TripCountry> newTripCountries = request.countries().stream()
                .map(dto -> {
                    Country country = countryRepository.findById(dto.countryId())
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST, "존재하지 않는 국가 ID입니다."));
                    return TripCountry.builder()
                            .trip(trip)
                            .country(country)
                            .build();
                })
                .toList();

        trip.getTripCountries().addAll(newTripCountries);
    }

    //수정 전 여행 일정 불러오기
    @Transactional(readOnly = true)
    public ScheduleResponse getTripSchedule(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 여행이 존재하지 않습니다."));

        return new ScheduleResponse(trip.getStartDate(), trip.getEndDate());
    }
}
