package com.snapsplit.backend.feature.receipt.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptResponse {

    private String currency; // 통화
    private BigDecimal totalAmount; // 총액
    /** 아이템 (이름/금액) */
    private List<UiItem> items;

    @Getter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class UiItem {
        private String name; // 상품명
        private BigDecimal amount; // 금액
    }
}
