package com.snapsplit.backend.feature.settlement.service;

import com.snapsplit.backend.domain.expense.entity.Expense;
import com.snapsplit.backend.domain.expense.entity.Pay;
import com.snapsplit.backend.domain.expense.entity.Split;
import com.snapsplit.backend.domain.expense.repository.ExpenseRepository;
import com.snapsplit.backend.domain.expense.repository.PayRepository;
import com.snapsplit.backend.domain.expense.repository.SplitRepository;
import com.snapsplit.backend.domain.settlement.entity.Settlement;
import com.snapsplit.backend.domain.settlement.entity.SettlementDetail;
import com.snapsplit.backend.domain.settlement.repository.SettlementDetailRepository;
import com.snapsplit.backend.domain.settlement.repository.SettlementRepository;
import com.snapsplit.backend.domain.tripmember.entity.MemberType;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.feature.settlement.dto.SettlementDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.snapsplit.backend.domain.expense.entity.Pay.MemberType.SHARED_FUND;

@Service
@RequiredArgsConstructor
public class SettlementDetailService {

    private final SettlementDetailRepository settlementDetailRepository;
    private final TripMemberRepository tripMemberRepository;
    private final SettlementRepository settlementRepository;
    private final SplitRepository splitRepository;
    private final ExpenseRepository expenseRepository;
    private final PayRepository payRepository;

    public SettlementDetailResponse getSettlementDetails(Long tripId, Long settlementId) {

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산이 존재하지 않습니다."));

        // 정산 상세 내역 조회
        List<SettlementDetail> details = settlementDetailRepository.findAllBySettlementId(settlementId);

        // TripMember 정보 전체 조회
        List<TripMember> tripMembers = tripMemberRepository.findAllByTripId(tripId);

        Map<Long, SettlementDetailResponse.Member> memberMap = tripMembers.stream()
                .collect(Collectors.toMap(
                        TripMember::getId,
                        tm -> SettlementDetailResponse.Member.builder()
                                .memberId(tm.getId())
                                .name(tm.getMemberType() == MemberType.SHARED_FUND
                                        ? "공동경비"
                                        : tm.getUser().getName())
                                .build()
                ));

        // 정산 대상 날짜 구간 정의
        LocalDate from = settlement.getStartDate().minusDays(1); // 시작일 하루 전
        LocalDate to = settlement.getEndDate(); // 종료일

        // startDate - 1 ~ endDate까지의 유효한 지출 받아오기
        List<Expense> expenses = expenseRepository.findByTripIdAndExpenseDateBetween(tripId, from, to);
        List<Long> expenseIds = expenses.stream().map(Expense::getId).toList();

        // expenseId들에 대한 pay 및 split 검색
        List<Pay> pays = payRepository.findAllByExpenseIdIn(expenseIds);
        List<Split> splits = splitRepository.findAllByExpenseIdIn(expenseIds);

        // USER별 지출 합계 (split 기준)
        Map<Long, BigDecimal> personalExpenseMap = splits.stream()
                .collect(Collectors.groupingBy(
                        Split::getSplitterId,
                        Collectors.reducing(BigDecimal.ZERO, Split::getSplitAmountKrw, BigDecimal::add)
                ));

        // 공동경비 지출 합계 (pay 기준)
        Map<Long, BigDecimal> sharedFundPayMap = pays.stream()
                .filter(pay -> pay.getPayer().getMemberType() == MemberType.SHARED_FUND)
                .collect(Collectors.groupingBy(
                        pay -> pay.getPayer().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Pay::getPayAmountKrw, BigDecimal::add)
                ));


        // personalExpenses 생성
        List<SettlementDetailResponse.PersonalExpense> personalExpenses = tripMembers.stream()
                .map(tm -> {
                    BigDecimal amount;

                    if (tm.getMemberType() == MemberType.SHARED_FUND) {
                        amount = sharedFundPayMap.getOrDefault(tm.getId(), BigDecimal.ZERO);
                    } else {
                        amount = personalExpenseMap.getOrDefault(tm.getId(), BigDecimal.ZERO);
                    }

                    return SettlementDetailResponse.PersonalExpense.builder()
                            .memberId(tm.getId())
                            .name(tm.getMemberType() == MemberType.SHARED_FUND
                                    ? "공동경비"
                                    : tm.getUser().getName())
                            .amount(amount)
                            .memberType(tm.getMemberType().name().toLowerCase()) // "user" 또는 "shared_fund"
                            .build();
                }).toList();

        // USER만 추려서 쌍 조합 생성
        List<TripMember> userMembers = tripMembers.stream()
                .filter(tm -> tm.getMemberType() == MemberType.USER)
                .toList();

        // 정산 상세 Map (senderId-receiverId → amount)
        Map<String, BigDecimal> detailMap = details.stream()
                .collect(Collectors.toMap(
                        d -> d.getSender().getId() + "-" + d.getReceiver().getId(),
                        SettlementDetail::getAmount
                ));

        // 모든 쌍을 순회하며 amount 채워넣기 (없으면 0으로)
        List<SettlementDetailResponse.SettlementDetail> detailResponses = new ArrayList<>();

        for (TripMember sender : userMembers) {
            for (TripMember receiver : userMembers) {
                if (sender.getId().equals(receiver.getId())) continue;

                BigDecimal amount = detailMap.getOrDefault(
                        sender.getId() + "-" + receiver.getId(),
                        BigDecimal.ZERO
                );

                detailResponses.add(SettlementDetailResponse.SettlementDetail.builder()
                        .sender(memberMap.get(sender.getId()))
                        .receiver(memberMap.get(receiver.getId()))
                        .amount(amount)
                        .build());
            }
        }

        // totalAmount 계산
        BigDecimal totalAmount = personalExpenses.stream()
                .map(SettlementDetailResponse.PersonalExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SettlementDetailResponse.builder()
                .id(settlementId)
                .members(
                        tripMembers.stream()
                                .filter(tm -> tm.getMemberType() == MemberType.USER)
                                .map(tm -> memberMap.get(tm.getId()))
                                .toList()
                )
                .settlementDetails(detailResponses)
                .personalExpenses(personalExpenses)
                .totalAmount(totalAmount)
                .build();
    }


}
