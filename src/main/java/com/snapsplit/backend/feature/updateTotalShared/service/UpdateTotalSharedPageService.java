package com.snapsplit.backend.feature.updateTotalShared.service;

import com.snapsplit.backend.domain.tripcountry.repository.TripCountryRepository;
import com.snapsplit.backend.feature.getExchangeRate.dto.ExchangeRateResponse;
import com.snapsplit.backend.feature.getExchangeRate.service.ExchangeRateService;
import com.snapsplit.backend.feature.updateTotalShared.dto.UpdateTotalSharedPageResponse;
import com.snapsplit.backend.feature.updateTotalShared.dto.UpdateTotalSharedPageResponse.CurrencyRate;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpdateTotalSharedPageService {

    private final TripRepository tripRepository;
    private final TripCountryRepository tripCountryRepository;
    private final ExchangeRateService exchangeRateService;

    public UpdateTotalSharedPageResponse getPageData(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        // 대표 통화
        String defaultCurrency = trip.getDefaultCurrency();

        // 사용 가능한 통화 (여행 국가의 통화 + 무조건 KRW 포함)
        Set<String> currencySet = tripCountryRepository.findAllByTripId(trip.getId()).stream()
                .map(tc -> tc.getCountry().getCurrency())
                .collect(Collectors.toSet());
        currencySet.add("KRW"); // 항상 포함
        List<String> availCurrencies = new ArrayList<>(currencySet);

        // 환율 정보 조회 + CurrencyRate 객체로 변환
        ExchangeRateResponse response = exchangeRateService.fetchExchangeRate(availCurrencies);

        List<CurrencyRate> currencies = response.getRates().stream()
                .map(item -> CurrencyRate.builder()
                        .code(item.getCode())
                        .exchangeRate(item.getRateToBase())
                        .build())
                .collect(Collectors.toList());

        // 응답 반환
        return UpdateTotalSharedPageResponse.builder()
                .defaultCurrency(defaultCurrency)
                .currencies(currencies)
                .build();
    }

}
