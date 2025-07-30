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
public class RemoveTotalSharedService {

    private final TripRepository tripRepository;
    private final SharedRepository sharedRepository;
    private final TotalSharedRepository totalSharedRepository;

    @Transactional
    public AddTotalSharedResponse removeTotalShared(Long tripId, AddTotalSharedRequest request) {
        // 여행 검증
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        // TotalShared 존재 확인
        TotalShared totalShared = totalSharedRepository.findByTripAndTotalSharedCurrency(trip, request.getCurrency())
                .orElseThrow(() -> new IllegalArgumentException("해당 통화의 총액 정보가 존재하지 않습니다."));

        BigDecimal currentAmount = totalShared.getTotalSharedAmount();

        // 빼려는 금액이 현재 금액보다 크면 예외
        if (request.getAmount().compareTo(currentAmount) > 0) {
            throw new IllegalArgumentException("회수 금액이 현재 공동경비 총액을 초과할 수 없습니다.");
        }

        // 환율 반영한 한화 금액 계산
        BigDecimal amountKRW = request.getAmount().multiply(request.getExchangeRate());

        // Shared에 음수 금액으로 회수 내역 저장
        Shared shared = Shared.builder()
                .trip(trip)
                .amount(request.getAmount().negate()) // 음수 처리
                .amountKRW(amountKRW)
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .createdAt(request.getCreatedAt())
                .build();
        sharedRepository.save(shared);

        // TotalShared 금액 차감
        BigDecimal newAmount = totalShared.getTotalSharedAmount().subtract(request.getAmount());
        totalShared.updateTotalSharedAmount(newAmount);
        totalShared.updateLatestModified(LocalDate.now());
        totalSharedRepository.save(totalShared);

        // 응답
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
