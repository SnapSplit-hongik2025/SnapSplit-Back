package com.snapsplit.backend.feature.editTrip.service;
import com.snapsplit.backend.domain.album.repository.AlbumRepository;
import com.snapsplit.backend.domain.country.entity.Country;
import com.snapsplit.backend.domain.country.repository.CountryRepository;
import com.snapsplit.backend.domain.photo.entity.Photo;
import com.snapsplit.backend.domain.photo.repository.PhotoRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.tripcountry.entity.TripCountry;
import com.snapsplit.backend.domain.expense.repository.ExpenseRepository;
import com.snapsplit.backend.feature.addExpense.service.AddExpenseService;
import com.snapsplit.backend.feature.editTrip.dto.*;
import com.snapsplit.backend.global.s3.S3Uploader;
import com.snapsplit.backend.global.s3.dto.S3UploadResult;
import com.snapsplit.backend.domain.settlement.repository.SettlementDetailRepository;
import com.snapsplit.backend.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditTripService {

    private final TripRepository tripRepository;
    private final CountryRepository countryRepository;
    private final S3Uploader s3Uploader;
    private final PhotoRepository photoRepository;
    private final AlbumRepository albumRepository;
    private final ExpenseRepository expenseRepository;
    private final AddExpenseService addExpenseService;
    private final RedisTemplate<String, String> redisTemplate;
    private final SettlementDetailRepository settlementDetailRepository;
    private final SettlementRepository settlementRepository;


    // 수정 전 여행지 불러오기
    @Transactional(readOnly = true)
    public CountriesResponse getTripCountries(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 여행이 존재하지 않습니다."));

        //선택된 국가 목록
        List<CountriesResponse.CountryDto> selectedCountries = trip.getTripCountries().stream()
                .map(tripCountry -> new CountriesResponse.CountryDto(
                        tripCountry.getCountry().getId(),
                        tripCountry.getCountry().getCountryName()
                ))
                .toList();

        //전체 국가 목록
        List<CountriesResponse.CountryDto> countries = countryRepository.findAll().stream()
                .map(country -> new CountriesResponse.CountryDto(
                        country.getId(),
                        country.getCountryName()
                ))
                .toList();

        return new CountriesResponse(countries, selectedCountries);
    }

    // 여행지 수정하기
    @Transactional
    public void updateCountries(Long tripId, UpdateCountriesRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 여행이 존재하지 않습니다."));

        // 기존 여행지 목록 초기화 (orphanRemoval = true 자동 삭제)
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

        // 여행 국가가 변경되면 대표 통화를 다시 KRW로 초기화
        trip.setDefaultCurrency("KRW");
    }

    // 수정 전 여행 일정 불러오기
    @Transactional(readOnly = true)
    public ScheduleResponse getTripSchedule(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 여행이 존재하지 않습니다."));

        return new ScheduleResponse(trip.getStartDate(), trip.getEndDate());
    }

    // 여행 일정 수정하기
    @Transactional
    public void updateTripSchedule(Long tripId, UpdateScheduleRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 여행이 존재하지 않습니다."));

        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
    }

    // 수정 전 여행명 및 대표 사진 불러오기
    @Transactional(readOnly = true)
    public TripInfoResponse getTripInfo(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 여행이 존재하지 않습니다."));

        return new TripInfoResponse(
                trip.getTripName(),
                trip.getTripImage()
        );
    }

    // 여행명 및 대표 사진 수정하기
    @Transactional
    public void updateTripInfo(Long tripId, String tripName, MultipartFile tripImageFile) throws IOException {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 여행이 존재하지 않습니다."));

        // 여행 이름 수정
        if (tripName != null && !tripName.trim().isEmpty()) {
            trip.setTripName(tripName);
        }

        // 대표 이미지 수정
        if (tripImageFile != null && !tripImageFile.isEmpty()) {
            S3UploadResult uploadResult = s3Uploader.upload(tripImageFile, "trip-images");
            trip.setTripImage(uploadResult.getFileUrl());
        }
    }



    // 여행 삭제하기
    @Transactional
    public void deleteTrip(Long tripId) {
        Trip trip = tripRepository.findTripWithAlbum(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "해당 여행이 존재하지 않습니다."));

        // 1. S3 사진 삭제만
        albumRepository.findByTripId(tripId).ifPresent(album -> {
            List<Photo> photos = photoRepository.findByAlbum_Id(album.getId());
            for (Photo photo : photos) {
                try {
                    s3Uploader.deleteByUrl(photo.getS3Url());
                } catch (Exception e) {
                    log.warn("S3 삭제 실패: {}", photo.getS3Url(), e);
                }
            }
        });


        // 2. Expense 전체 삭제
        expenseRepository.findAllByTripId(tripId)
                .forEach(expense -> addExpenseService.deleteExpense(tripId, expense.getId()));

        //TripCountry, TripMember, Shared, TotalShared -> cascade로 자동 삭제
        // 3-1) SettlementDetail 삭제
        settlementDetailRepository.deleteByTripId(tripId);

        // 3-2) Settlement 삭제
        settlementRepository.deleteByTripId(tripId);


        // 4. Redis 캐시 삭제
        redisTemplate.delete("trip::" + tripId + "::snapReadiness");

        // 5. Trip 삭제
        tripRepository.delete(trip);
    }
}
