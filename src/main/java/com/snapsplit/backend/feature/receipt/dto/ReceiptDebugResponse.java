// ReceiptDebugResponse.java
package com.snapsplit.backend.feature.receipt.dto;

import lombok.*;
import java.util.List;

@Getter @Builder @AllArgsConstructor @NoArgsConstructor
public class ReceiptDebugResponse {
    private String rawText;           // 원문 텍스트 (길이 제한)
    private boolean rawTextTruncated; // 잘렸는지 표시
    private String documentJson;      // Document 전체 JSON (길이 제한)
    private boolean documentJsonTruncated;
    private int pageCount;            // 페이지 수
    private int entityCount;          // 엔티티 총개수(실제)
    private List<EntityRow> entities; // 엔티티 요약 표 (상위 N개)

    @Getter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class EntityRow {
        private String type;              // e.g. total, line_item, amount
        private String mentionText;       // 화면 보인 텍스트
        private String normalizedText;    // normalized_value.text
        private String moneyCurrency;     // normalized_value.money_value.currency_code
        private String moneyAmount;       // normalized_value.money_value (units+nanos)
        private Double confidence;        // 엔티티 confidence
        private int propertyCount;        // 하위 속성 개수
        private String pageRefs;          // 페이지 앵커 간단 요약
    }
}
