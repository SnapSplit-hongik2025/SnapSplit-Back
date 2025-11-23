package com.snapsplit.backend.feature.tripHome.service;

import com.snapsplit.backend.domain.expense.entity.CategoryExpense;
import com.snapsplit.backend.domain.expense.entity.Expense;
import com.snapsplit.backend.domain.expense.entity.Split;
import com.snapsplit.backend.domain.expense.repository.CategoryExpenseRepository;
import com.snapsplit.backend.domain.expense.repository.ExpenseRepository;
import com.snapsplit.backend.domain.expense.repository.SplitRepository;
import com.snapsplit.backend.domain.totalshared.entity.TotalShared;
import com.snapsplit.backend.domain.totalshared.repository.TotalSharedRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.feature.tripHome.dto.TripHomeResponse;
import com.snapsplit.backend.feature.tripHome.dto.TripHomeResponse.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripHomeService {

    private final TripRepository tripRepository;
    private final TotalSharedRepository totalSharedRepository;
    private final CategoryExpenseRepository categoryExpenseRepository;
    private final ExpenseRepository expenseRepository;
    private final SplitRepository splitRepository;
    private final TripMemberRepository tripMemberRepository;


    @Transactional(readOnly=true)
    public TripHomeResponse getTripHome(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        // countries: TripCountry → Country → countryName
        List<String> countries = trip.getTripCountries().stream()
                .map(tripCountry -> tripCountry.getCountry().getCountryName())
                .toList();

        // 여행 멤버 프로필 이미지
        List<String> memberProfileImages = trip.getTripMembers().stream()
                .filter(tm -> tm.getUser() != null)         // 공동경비 제외, 실제 유저만
                .map(tm -> tm.getUser().getProfileImage())
                .filter(Objects::nonNull)                   // 프로필 없는 경우 제외
                .toList();

        // 공동경비 정보 (대표통화 기준)
        TotalShared totalShared = totalSharedRepository
                .findByTripAndTotalSharedCurrency(trip, trip.getDefaultCurrency())
                .orElse(null);

        SharedFundDto sharedFund = SharedFundDto.builder()
                .defaultCurrency(trip.getDefaultCurrency())
                .balance(totalShared != null ? totalShared.getTotalSharedAmount() : BigDecimal.ZERO)
                .build();

        // 가장 큰 카테고리 지출
        List<CategoryExpense> categoryExpenses = categoryExpenseRepository.findByTrip(trip);

        CategoryExpense topCategory = categoryExpenses.stream()
                .max(Comparator.comparing(CategoryExpense::getAmountKRW))
                .orElse(null);

        TopCategoryExpenseDto topCategoryExpense = topCategory != null ?
                TopCategoryExpenseDto.builder()
                        .category(topCategory.getCategory().name())
                        .amountKRW(topCategory.getAmountKRW())
                        .build()
                : null;

        // 전체 지출
        List<Expense> expenses = expenseRepository.findAllByTripId(tripId);

        // 지출 ID로 모든 split 한 번에  조회
        List<Long> expenseIds = expenses.stream().map(Expense::getId).toList();
        List<Split> allSplits = splitRepository.findByExpenseIdIn(expenseIds);

        // splitterId -> name Map 만들기
        Set<Long> splitterIds = allSplits.stream()
                .map(Split::getSplitterId)
                .collect(Collectors.toSet());

        Map<Long, String> splitterNameMap = tripMemberRepository.findAllById(splitterIds).stream()
                .collect(Collectors.toMap(
                        TripMember::getId,
                        tm -> tm.getUser().getName()
                ));

        // 전체 기간 날짜 구간 생성
        LocalDate startDate = trip.getStartDate();
        LocalDate dayBeforeStart = startDate.minusDays(1);

        List<LocalDate> allDates = dayBeforeStart
                .datesUntil(trip.getEndDate().plusDays(1))
                .toList();

        // 날짜별 지출 그룹핑
        Map<LocalDate, List<Expense>> expensesByDate = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getExpenseDate));

        // expense ID별로 splits를 그룹화 (미리 처리)
        Map<Long, List<Split>> splitsByExpenseId = allSplits.stream()
                .collect(Collectors.groupingBy(Split::getExpenseId));

        // 날짜별 지출 리스트 생성 (지출 없는 날짜도 포함)
        List<DailyExpenseDto> dailyExpenses = allDates.stream()
                .map(date -> {
                    List<Expense> expensesOnDate = expensesByDate.getOrDefault(date, Collections.emptyList());

                    List<ExpenseDto> expenseDtos = expensesOnDate.stream()
                            .map(expense -> {
                                List<String> splitterNames = splitsByExpenseId.getOrDefault(expense.getId(), Collections.emptyList())
                                        .stream()
                                        .map(split -> splitterNameMap.get(split.getSplitterId()))
                                        .filter(Objects::nonNull)
                                        .toList();

                                return ExpenseDto.builder()
                                        .expenseId(expense.getId())
                                        .category(expense.getCategory().name())
                                        .expenseName(expense.getExpenseName())
                                        .expenseMemo(expense.getExpenseMemo())
                                        .amount(expense.getExpenseAmount())
                                        .currency(expense.getExpenseCurrency())
                                        .splitters(splitterNames)
                                        .build();
                            })
                            .toList();

                    return DailyExpenseDto.builder()
                            .date(date)
                            .expenses(expenseDtos)
                            .build();
                })
                .toList();


        // 전체 지출 총합 (KRW)
        BigDecimal totalExpenseKRW = categoryExpenses.stream()
                .map(CategoryExpense::getAmountKRW)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TripHomeResponse.builder()
                .tripId(trip.getId())
                .tripName(trip.getTripName())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .countries(countries)
                .memberProfileImages(memberProfileImages)
                .sharedFund(sharedFund)
                .topCategoryExpense(topCategoryExpense)
                .dailyExpenses(dailyExpenses)
                .totalExpense(totalExpenseKRW)
                .build();
    }
}
