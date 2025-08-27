package com.snapsplit.backend.feature.receipt.service;

import com.google.protobuf.util.JsonFormat;
import com.google.cloud.documentai.v1.*;
import com.google.protobuf.ByteString;
import com.snapsplit.backend.feature.receipt.dto.ReceiptDebugResponse;
import com.snapsplit.backend.feature.receipt.dto.ReceiptRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceiptDebug {

    private final DocumentProcessorServiceClient client;

    @Value("${gcp.docai.project-id}")
    private String projectId;

    @Value("${gcp.docai.location}")
    private String location;

    @Value("${gcp.docai.processor-id}")
    private String processorId;

    public ReceiptDebugResponse debug(ReceiptRequest request, boolean pretty, int entitiesLimit) {
        try {
            MultipartFile file = request.getFile();
            String mimeType = (file.getContentType() != null) ? file.getContentType() : "image/jpeg";

            String processor = ProcessorName.of(projectId, location, processorId).toString();
            RawDocument rawDocument = RawDocument.newBuilder()
                    .setContent(ByteString.copyFrom(file.getBytes()))
                    .setMimeType(mimeType)
                    .build();

            ProcessRequest pr = ProcessRequest.newBuilder()
                    .setName(processor)
                    .setRawDocument(rawDocument)
                    .build();

            ProcessResponse resp = client.processDocument(pr);
            Document doc = resp.getDocument();

            // 1) raw text (길이 제한)
            final int RAW_LIMIT = 200_000; // 200KB 정도
            String rawText = doc.getText();
            boolean rawTrunc = rawText != null && rawText.length() > RAW_LIMIT;
            rawText = clamp(rawText, RAW_LIMIT);

            // 2) 전체 Document JSON (길이 제한)
            String json;
            JsonFormat.Printer printer = JsonFormat.printer().includingDefaultValueFields();
            // JsonFormat 기본은 pretty 없음 → 필요하면 그대로(긴 한 줄). pretty 원하면 가볍게 줄바꿈만 처리
            json = printer.print(doc);
            if (pretty) json = json.replaceAll(",", ",\n"); // 매우 러프한 보기용 개행 (원하면 Jackson으로 포매팅해도 됨)

            final int JSON_LIMIT = 1_000_000; // 1MB
            boolean jsonTrunc = json.length() > JSON_LIMIT;
            json = clamp(json, JSON_LIMIT);

            // 3) 엔티티 요약 표 (상위 N개)
            List<ReceiptDebugResponse.EntityRow> rows = new ArrayList<>();
            int totalEntities = doc.getEntitiesCount();
            int cap = Math.max(1, Math.min(entitiesLimit, totalEntities));
            for (int i = 0; i < cap; i++) {
                Document.Entity e = doc.getEntities(i);
                String type = nte(e.getType());
                String mention = nte(e.getMentionText());
                String normText = e.hasNormalizedValue() ? nte(e.getNormalizedValue().getText()) : null;
                String moneyCode = null;
                String moneyAmount = null;
                if (e.hasNormalizedValue() && e.getNormalizedValue().hasMoneyValue()) {
                    com.google.type.Money m = e.getNormalizedValue().getMoneyValue();
                    moneyCode = nte(m.getCurrencyCode());
                    // units + nanos(9자리) 조합
                    moneyAmount = m.getUnits() + (m.getNanos() == 0 ? "" : ("." + String.format("%09d", m.getNanos())));
                    // 소수점 우측 불필요한 0 제거
                    moneyAmount = moneyAmount.replaceAll("\\.?0+$", "");
                }
                String pageRefs = "";
                if (e.hasPageAnchor()) {
                    var pa = e.getPageAnchor();
                    if (pa.getPageRefsCount() > 0) {
                        StringBuilder sb = new StringBuilder();
                        for (int k = 0; k < Math.min(3, pa.getPageRefsCount()); k++) {
                            var ref = pa.getPageRefs(k);
                            sb.append("#").append(ref.getPage()).append(" ");
                        }
                        pageRefs = sb.toString().trim();
                    }
                }
                rows.add(ReceiptDebugResponse.EntityRow.builder()
                        .type(type)
                        .mentionText(mention)
                        .normalizedText(normText)
                        .moneyCurrency(moneyCode)
                        .moneyAmount(moneyAmount)
                        .confidence((double) e.getConfidence())
                        .propertyCount(e.getPropertiesCount())
                        .pageRefs(pageRefs)
                        .build());
            }

            return ReceiptDebugResponse.builder()
                    .rawText(rawText)
                    .rawTextTruncated(rawTrunc)
                    .documentJson(json)
                    .documentJsonTruncated(jsonTrunc)
                    .pageCount(doc.getPagesCount())
                    .entityCount(totalEntities)
                    .entities(rows)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("OCR 디버그 조회 실패: " + e.getMessage(), e);
        }
    }

    private static String clamp(String s, int limit) {
        if (s == null) return null;
        return s.length() <= limit ? s : s.substring(0, limit);
    }
    private static String nte(String s) { return (s == null || s.isBlank()) ? null : s; }
}
