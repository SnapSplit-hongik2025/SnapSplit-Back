package com.snapsplit.backend.feature.updateTotalShared.service;

import com.snapsplit.backend.domain.shared.entity.Shared;
import com.snapsplit.backend.domain.shared.repository.SharedRepository;
import com.snapsplit.backend.domain.totalshared.entity.TotalShared;
import com.snapsplit.backend.domain.totalshared.repository.TotalSharedRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.feature.updateTotalShared.dto.AddTotalSharedRequest;
import com.snapsplit.backend.feature.updateTotalShared.dto.AddTotalSharedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AddTotalSharedService {

    private final TripRepository tripRepository;
    private final SharedRepository sharedRepository;
    private final TotalSharedRepository totalSharedRepository;

    @Transactional
    public AddTotalSharedResponse addTotalShared(Long tripId, AddTotalSharedRequest request) {

        // 해당 여행 찾기
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        // 환율 반영한 한화 금액 계산
        BigDecimal amountKRW = request.getAmount().multiply(request.getExchangeRate());

        // Shared 저장
        Shared shared = Shared.builder()
                .trip(trip)
                .amount(request.getAmount())
                .amountKRW(amountKRW)
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .createdAt(request.getCreatedAt())
                .build();
        sharedRepository.save(shared);

        // TotalShared 업데이트 또는 신규 생성
        TotalShared totalShared = totalSharedRepository.findByTripAndTotalSharedCurrency(trip, request.getCurrency())
                .orElse(TotalShared.builder()
                        .trip(trip)
                        .totalSharedAmount(BigDecimal.ZERO)
                        .totalSharedCurrency(request.getCurrency())
                        .latestModified(LocalDate.now())
                        .build());

        // 경비총액과 최신 입금일자 변경
        totalShared.updateTotalSharedAmount(totalShared.getTotalSharedAmount().add(request.getAmount()));
        totalShared.updateLatestModified(LocalDate.now());

        // 저장
        totalSharedRepository.save(totalShared);

        // 응답 빌드
        return AddTotalSharedResponse.builder()
                .sharedId(shared.getId())
                .tripId(trip.getId())
                .amount(shared.getAmount())
                .currency(shared.getCurrency())
                .paymentMethod(shared.getPaymentMethod())
                .createdAt(shared.getCreatedAt())
                .build();
    }
}
