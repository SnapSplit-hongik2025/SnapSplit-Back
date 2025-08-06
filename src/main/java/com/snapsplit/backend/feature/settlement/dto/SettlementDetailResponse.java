package com.snapsplit.backend.feature.settlement.dto;

import com.snapsplit.backend.domain.expense.entity.Pay;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SettlementDetailResponse {
    private Long id; // 정산 ID
    private List<Member> members; // 여행 멤버 리스트
    private List<SettlementDetail> settlementDetails; // 정산 세부 내역
    private List<PersonalExpense> personalExpenses; // 개인 지출 내역
    private BigDecimal totalAmount; // 총 지출 합계

    @Data
    @Builder
    public static class Member {
        private Long memberId; // 트립 멤버 아이디
        private String name; // 이름
    }

    @Data
    @Builder
    public static class SettlementDetail {
        private Member sender; // 보내는 사람
        private Member receiver; // 받는 사람
        private BigDecimal amount; // 금액
    }

    @Data
    @Builder
    public static class PersonalExpense {
        private Long memberId; // 트립 멤버 아이디
        private String name; // 이름
        private BigDecimal amount; // 금액
        private String memberType; // user / shared_fund
    }
}
