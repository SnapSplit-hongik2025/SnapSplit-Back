package com.snapsplit.backend.feature.addExpense.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapsplit.backend.domain.expense.entity.*;
import com.snapsplit.backend.domain.expense.repository.*;
import com.snapsplit.backend.domain.receipt.entity.Receipt;
import com.snapsplit.backend.domain.receipt.repository.ReceiptRepository;
import com.snapsplit.backend.domain.shared.entity.PaymentMethod;
import com.snapsplit.backend.domain.shared.entity.Shared;
import com.snapsplit.backend.domain.shared.repository.SharedRepository;
import com.snapsplit.backend.domain.totalshared.repository.TotalSharedRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.feature.addExpense.dto.AddExpenseRequest;
import com.snapsplit.backend.feature.addExpense.dto.ExpenseDetailResponse;
import com.snapsplit.backend.domain.shared.entity.SharedType;
import com.snapsplit.backend.feature.getCategoryExpense.service.CategoryExpenseService;
import com.snapsplit.backend.feature.receipt.service.ReceiptService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddExpenseService {

    private final ExpenseRepository expenseRepository;
    private final PayRepository payRepository;
    private final SplitRepository splitRepository;
    private final TripMemberRepository tripMemberRepository;
    private final SharedRepository sharedRepository;
    private final TotalSharedRepository totalSharedRepository;
    private final TripRepository tripRepository;
    private final CategoryExpenseService categoryExpenseService;
    private final ReceiptRepository receiptRepository;


    //지출 추가
    @Transactional
    public Long addExpense(Long tripId, AddExpenseRequest request) {
        var info = request.expense();
        BigDecimal rate = info.exchangeRate();
        BigDecimal expenseAmount = info.amount();

        // Trip 객체 조회 (공동경비 조회 및 Shared 저장용으로 사용)
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        // 모든 payer의 TripMember를 한 번에 조회
        List<Long> payerMemberIds = request.payers().stream()
                .map(AddExpenseRequest.PayerDto::memberId)
                .toList();
        Map<Long, TripMember> tripMemberMap = tripMemberRepository.findAllById(payerMemberIds)
                .stream()
                .collect(Collectors.toMap(TripMember::getId, Function.identity()));

        // 공동경비 제외한 사용자 pay 총합 계산
        BigDecimal userPayTotal = request.payers().stream()
                .filter(p -> {
                    TripMember tripMember = tripMemberMap.get(p.memberId());
                    if (tripMember == null) {
                        throw new EntityNotFoundException("해당 tripMember가 존재하지 않습니다.");
                    }
                    return tripMember.getUser() != null;
                })
                .map(AddExpenseRequest.PayerDto::payerAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // split 총합 계산
        BigDecimal splitTotal = request.splitters().stream()
                .map(AddExpenseRequest.SplitterDto::splitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!splitTotal.equals(userPayTotal)) {
            throw new IllegalArgumentException("splitList의 금액 합계가 사용자 결제 금액과 일치하지 않습니다.");
        }


        // pay 총합 검증
        BigDecimal payTotal = request.payers().stream()
                .map(AddExpenseRequest.PayerDto::payerAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!payTotal.equals(expenseAmount)) {
            throw new IllegalArgumentException("payList의 결제 금액 합계가 지출 금액과 일치하지 않습니다.");
        }

        // 지출 저장
        Expense expense = expenseRepository.save(
                Expense.builder()
                        .tripId(tripId)
                        .expenseDate(info.date())
                        .expenseAmount(expenseAmount)
                        .expenseCurrency(info.currency())
                        .expenseKrw(expenseAmount.multiply(rate))
                        .category(Expense.Category.valueOf(info.category().toUpperCase()))
                        .expenseName(info.expenseName())
                        .expenseMemo(info.expenseMemo())
                        .paymentMethod(Expense.PaymentMethod.valueOf(info.paymentMethod().toUpperCase()))
                        .build()
        );

        // 영수증으로 지출 추가한 경우
        if (request.receiptUrl() != null) {
            String extractedData = null;
            if (request.items() != null && !request.items().isEmpty()) {
                // ObjectMapper를 사용한 안전한 JSON 직렬화
                ObjectMapper mapper = new ObjectMapper();
                try {
                    extractedData = mapper.writeValueAsString(request.items());
                } catch (Exception e) {
                    throw new RuntimeException("영수증 아이템 직렬화 실패", e);
                }
            }

            Receipt receipt = Receipt.builder()
                    .expense(expense)
                    .receiptUrl(request.receiptUrl())
                    .extractedData(extractedData)
                    .build();

            receiptRepository.save(receipt);
        }

        // 결제자 저장 & 공동 경비 처리
        List<Pay> pays = request.payers().stream()
                .map(payer -> {
                    TripMember tripMember = tripMemberRepository.findById(payer.memberId())
                            .orElseThrow(() -> new EntityNotFoundException("해당 tripMember가 존재하지 않습니다."));

                    Pay.MemberType memberType = (tripMember.getUser() == null)
                            ? Pay.MemberType.SHARED_FUND
                            : Pay.MemberType.USER;

                    // 공동경비 결제자 처리
                    if (memberType == Pay.MemberType.SHARED_FUND) {
                        var totalShared = totalSharedRepository.findByTripAndTotalSharedCurrency(trip, info.currency())
                                .orElseThrow(() -> new IllegalArgumentException("공동경비가 존재하지 않습니다."));

                        BigDecimal current = totalShared.getTotalSharedAmount();
                        BigDecimal used = payer.payerAmount();

                        if (current.compareTo(used) < 0) {
                            throw new IllegalArgumentException("공동경비 잔액이 부족합니다.");
                        }

                        // 공동 경비 잔액 차감 및 최신 수정일 갱신
                        totalShared.updateTotalSharedAmount(current.subtract(used));
                        totalShared.updateLatestModified(java.time.LocalDate.now());
                        totalSharedRepository.save(totalShared);

                        //공동 경비 사용 이력 추가
                        sharedRepository.save(Shared.builder()
                                .trip(trip)
                                .amount(used.negate())
                                .amountKRW(used.negate().multiply(rate))
                                .currency(info.currency())
                                .paymentMethod(parsePaymentMethod(info.paymentMethod()))
                                .createdAt(java.time.LocalDate.now())
                                .sharedType(SharedType.EXPENSE)
                                .expenseId(expense.getId())
                                .build());
                    }

                    return Pay.builder()
                            .expenseId(expense.getId())
                            .payerId(payer.memberId())
                            .payAmount(payer.payerAmount())
                            .payAmountKrw(payer.payerAmount().multiply(rate))
                            .memberType(memberType)
                            .build();
                }).toList();
        payRepository.saveAll(pays);

        // 분담자 저장
        List<Split> splits = request.splitters().stream().map(splitter ->
                Split.builder()
                        .expenseId(expense.getId())
                        .splitterId(splitter.memberId())
                        .splitAmount(splitter.splitAmount())
                        .splitAmountKrw(splitter.splitAmount().multiply(rate))
                        .build()
        ).toList();
        splitRepository.saveAll(splits);

        //카테고리별 누적 지출 반영
        categoryExpenseService.updateOnExpenseAdd(
                tripId,
                expense.getCategory(),
                expense.getExpenseKrw()
        );
        return expense.getId();
    }


    //지출 상세보기
    @Transactional(readOnly = true)
    public ExpenseDetailResponse getExpenseDetail(Long tripId, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("해당 지출이 존재하지 않습니다."));

        if (!expense.getTripId().equals(tripId)) {
            throw new IllegalArgumentException("해당 여행의 지출이 아닙니다.");
        }

        List<ExpenseDetailResponse.MemberAmountDto> payers = payRepository.findByExpenseId(expenseId)
                .stream()
                .map(pay -> {
                    TripMember member = tripMemberRepository.findById(pay.getPayerId())
                            .orElseThrow(() -> new EntityNotFoundException("해당 결제자 멤버를 찾을 수 없습니다."));
                    String name = (member.getUser() != null) ? member.getUser().getName() : "공동경비";
                    return ExpenseDetailResponse.MemberAmountDto.builder()
                            .memberId(member.getId())
                            .name(name)
                            .amount(pay.getPayAmount())
                            .build();
                }).toList();

        List<ExpenseDetailResponse.MemberAmountDto> splitters = splitRepository.findByExpenseId(expenseId)
                .stream()
                .map(split -> {
                    TripMember member = tripMemberRepository.findById(split.getSplitterId())
                            .orElseThrow(() -> new EntityNotFoundException("해당 분담자 멤버를 찾을 수 없습니다."));
                    return ExpenseDetailResponse.MemberAmountDto.builder()
                            .memberId(member.getId())
                            .name(member.getUser().getName())
                            .amount(split.getSplitAmount())
                            .build();
                }).toList();

        // 영수증으로 지출 추가한 지출일 경우
        Receipt receipt = receiptRepository.findByExpense_Id(expenseId).orElse(null);
        String receiptUrl = null;
        List<ExpenseDetailResponse.ReceiptItemDto> receiptItems = null;

        if (receipt != null) {
            receiptUrl = receipt.getReceiptUrl();

            if (receipt.getExtractedData() != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    // ReceiptItemDto 바로 역직렬화
                    receiptItems = mapper.readValue(
                            receipt.getExtractedData(),
                            new TypeReference<List<ExpenseDetailResponse.ReceiptItemDto>>() {}
                    );
                } catch (Exception e) {
                    // 파싱 실패하면 null 유지
                }
            }
        }
        return ExpenseDetailResponse.builder()
                .expenseId(expense.getId())
                .amount(expense.getExpenseAmount())
                .amountKRW(expense.getExpenseKrw())
                .currency(expense.getExpenseCurrency())
                .paymentMethod(expense.getPaymentMethod().toString())
                .date(expense.getExpenseDate())
                .expenseName(expense.getExpenseName())
                .expenseMemo(expense.getExpenseMemo())
                .category(expense.getCategory().toString())
                .payers(payers)
                .splitters(splitters)
                .receiptUrl(receiptUrl)
                .receiptItems(receiptItems)
                .build();
    }

    //지출 삭제
    @Transactional
    public void deleteExpense(Long tripId, Long expenseId) {

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지출이 존재하지 않습니다."));

        if (!expense.getTripId().equals(tripId)) {
            throw new IllegalArgumentException("여행 정보가 일치하지 않습니다.");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행 정보가 존재하지 않습니다."));

        // 공동경비로 지불한 금액 복원
        List<Pay> pays = payRepository.findByExpenseId(expenseId);
        for (Pay pay : pays) {
            if (pay.getMemberType() == Pay.MemberType.SHARED_FUND) {
                var totalShared = totalSharedRepository.findByTripAndTotalSharedCurrency(trip, expense.getExpenseCurrency())
                        .orElseThrow(() -> new IllegalArgumentException("해당 통화의 공동경비가 존재하지 않습니다."));

                totalShared.updateTotalSharedAmount(totalShared.getTotalSharedAmount().add(pay.getPayAmount()));
                totalShared.updateLatestModified(java.time.LocalDate.now());
                totalSharedRepository.save(totalShared);
            }
        }

        payRepository.deleteByExpenseId(expenseId);
        splitRepository.deleteByExpenseId(expenseId);
        expenseRepository.deleteById(expenseId);

        sharedRepository.deleteByTripIdAndExpenseIdAndSharedType(
                tripId,
                expenseId,
                SharedType.EXPENSE
        );

        // 카테고리별 누적 지출 차감
        categoryExpenseService.updateOnExpenseDelete(
                tripId,
                expense.getCategory(),
                expense.getExpenseKrw()
        );
    }


    //지출 수정
    @Transactional
    public Long updateExpense(Long tripId, Long expenseId, AddExpenseRequest request) {
        deleteExpense(tripId, expenseId); // 기존 지출 삭제
        return addExpense(tripId, request); // 새 지출 등록
    }

    private PaymentMethod parsePaymentMethod(String value) {
        try {
            return PaymentMethod.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 결제 방식입니다: " + value);
        }
    }


}
