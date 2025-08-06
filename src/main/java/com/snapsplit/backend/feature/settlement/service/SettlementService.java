package com.snapsplit.backend.feature.settlement.service;

import com.snapsplit.backend.domain.expense.entity.Expense;
import com.snapsplit.backend.domain.expense.entity.Pay;
import com.snapsplit.backend.domain.expense.entity.Split;
import com.snapsplit.backend.domain.expense.repository.ExpenseRepository;
import com.snapsplit.backend.domain.expense.repository.PayRepository;
import com.snapsplit.backend.domain.expense.repository.SplitRepository;
import com.snapsplit.backend.domain.settlement.entity.Settlement;
import com.snapsplit.backend.domain.settlement.entity.SettlementDetail;
import com.snapsplit.backend.domain.settlement.repository.SettlementRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.feature.settlement.dto.SettlementDetailResponse;
import com.snapsplit.backend.feature.settlement.dto.SettlementRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static com.snapsplit.backend.domain.expense.entity.Pay.MemberType.SHARED_FUND;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final PayRepository payRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final SplitRepository splitRepository;

    public Long createSettlement(Long tripId, SettlementRequest request) {

        // 정산 시작일 및 종료일
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        // 여행 찾기
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행입니다."));

        // 정산 대상 멤버 가져오기
        List<TripMember> tripMembers = tripMemberRepository.findAllByTripId(tripId);

        // startDate - 1 ~ endDate까지의 유효한 지출 받아오기
        List<Expense> expenses = expenseRepository.findByTripIdAndExpenseDateBetween(tripId, startDate.minusDays(1), endDate);
        List<Long> expenseIds = expenses.stream().map(Expense::getId).toList();

        // expenseId들에 대한 pay 및 split 검색
        List<Pay> pays = payRepository.findAllByExpenseIdIn(expenseIds);
        List<Split> splits = splitRepository.findAllByExpenseIdIn(expenseIds);

        // 각자가 결제한 금액 계산
        Map<Long, BigDecimal> paidMap = new HashMap<>();

        for (Pay pay : pays) {
            if (pay.getMemberType() == SHARED_FUND) continue; // 공동경비는 제외
            if (pay.getPayer() == null) {
                continue;
            }
            Long memberId = pay.getPayer().getId();
            paidMap.put(memberId,
                    paidMap.getOrDefault(memberId, BigDecimal.ZERO).add(pay.getPayAmountKrw()));
        }

        // 각자가 쓴 금액 계산
        Map<Long, BigDecimal> spentMap = new HashMap<>();

        for (Split split : splits) {
            Long memberId = split.getSplitterId();
            BigDecimal amount = split.getSplitAmountKrw();

            spentMap.put(memberId,
                    spentMap.getOrDefault(memberId, BigDecimal.ZERO).add(amount));
        }

        // netBalance 계산
        // +인 사람을 받아야 하고, -인 사람은 줘야 함
        Map<Long, BigDecimal> netBalanceMap = new HashMap<>();

        for (TripMember member : tripMembers) {
            Long memberId = member.getId();

            BigDecimal paid = paidMap.getOrDefault(memberId, BigDecimal.ZERO);
            BigDecimal spent = spentMap.getOrDefault(memberId, BigDecimal.ZERO);
            BigDecimal net = paid.subtract(spent);

            netBalanceMap.put(memberId, net);
        }

        // 받을 사람, 낼 사람 나누기
        List<Map.Entry<Long, BigDecimal>> receivers = new ArrayList<>(
                netBalanceMap.entrySet().stream()
                        .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .toList()
        );

        List<Map.Entry<Long, BigDecimal>> payers = new ArrayList<>(
                netBalanceMap.entrySet().stream()
                        .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) < 0)
                        .sorted(Map.Entry.comparingByValue())
                        .toList()
        );


        // 정산 세부내역 리스트 생성
        List<SettlementDetail> details = new ArrayList<>();

        int i = 0, j = 0;

        while (i < payers.size() && j < receivers.size()) {
            var payer = payers.get(i);
            var receiver = receivers.get(j);

            Long payerId = payer.getKey();
            Long receiverId = receiver.getKey();
            BigDecimal payerAmount = payer.getValue().abs(); // -30 → 30
            BigDecimal receiverAmount = receiver.getValue();

            BigDecimal transfer = payerAmount.min(receiverAmount); // 갚을 수 있는 만큼만

            details.add(SettlementDetail.builder()
                    .sender(findTripMemberById(tripMembers, payerId))
                    .receiver(findTripMemberById(tripMembers, receiverId))
                    .amount(transfer)
                    .build());

            // 업데이트
            payers.set(i, Map.entry(payerId, payer.getValue().add(transfer)));
            receivers.set(j, Map.entry(receiverId, receiverAmount.subtract(transfer)));

            if (payers.get(i).getValue().compareTo(BigDecimal.ZERO) == 0) i++;
            if (receivers.get(j).getValue().compareTo(BigDecimal.ZERO) == 0) j++;
        }

        Settlement settlement = Settlement.builder()
                .trip(trip)
                .startDate(startDate)
                .endDate(endDate)
                .createdAt(LocalDate.now())
                .build();

        // 양방향 연관관계 설정
        for (SettlementDetail detail : details) {
            detail.setSettlement(settlement);
            settlement.getDetails().add(detail);
        }

        settlementRepository.save(settlement);
        return settlement.getId();

    }

    private TripMember findTripMemberById(List<TripMember> members, Long id) {
        return members.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("TripMember not found: " + id));
    }

}
