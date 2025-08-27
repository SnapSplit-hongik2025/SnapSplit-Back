package com.snapsplit.backend.feature.receipt.service;

import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessorName;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.documentai.v1.RawDocument;
import com.snapsplit.backend.feature.receipt.dto.ReceiptRequest;
import com.snapsplit.backend.feature.receipt.dto.ReceiptResponse;
import com.google.protobuf.ByteString;
import com.snapsplit.backend.global.exception.ReceiptProcessingException;
import com.snapsplit.backend.global.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final DocumentProcessorServiceClient client;
    private final S3Uploader s3Uploader;

    @Value("${gcp.docai.project-id}")
    private String projectId;

    @Value("${gcp.docai.location}")
    private String location;

    @Value("${gcp.docai.processor-id}")
    private String processorId;

    // ===================== Public API =====================

    /**
     * 영수증 이미지를 DocAI Expense Parser로 처리하고,
     * - 통화(currency): 엔티티/원문 힌트에서 추출
     * - 아이템(items): line_item 엔티티만 보고 단순 규칙으로 추출
     * - 총액(totalAmount): "항상" 아이템 금액 합계로 산출
     */
    public ReceiptResponse parse(ReceiptRequest request) {
        try {
            MultipartFile file = request.getFile();

            String receiptUrl;
            try {
                com.snapsplit.backend.global.s3.dto.S3UploadResult uploadResult = s3Uploader.upload(file, "receipt-images");
                receiptUrl = uploadResult.getFileUrl();
            } catch (IOException e) {
                throw new ReceiptProcessingException("영수증 이미지 업로드에 실패했습니다.", e);
            }

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

            Document doc;
            try {
                ProcessResponse resp = client.processDocument(pr);
                doc = resp.getDocument();
            } catch (com.google.api.gax.rpc.ApiException ex) {
                // 네트워크/서비스 오류 → 502 매핑 (rate limit/exhaustion은 필요시 503 고려)
                throw new ReceiptProcessingException("DocAI 호출 실패");
            }


            // rawText: 통화 추정에만 사용
            String rawText = safeClamp(doc.getText(), 20_000);

            // ---- Currency (코드 > 심볼 > hint) ----
            String currencyCodeFromEntities = pickCurrencyCode(doc);
            String currencyCodeFromRaw      = detectCurrencyCodeFromRaw(rawText);
            String currencySymbol           = firstNonEmpty(
                    getEntityTextAny(doc, "currency_symbol", "currency"),
                    detectCurrencySymbolFromRaw(rawText),
                    symbolFromCurrencyCode(currencyCodeFromEntities),
                    symbolFromCurrencyCode(currencyCodeFromRaw)
            );
            String currencyFinal = firstNonEmpty(
                    currencyCodeFromEntities,
                    currencyCodeFromRaw,
                    currencySymbol
            );

            // ---- Items: line_item만 보고 단순 규칙으로 뽑기 (기존)
            List<ReceiptResponse.UiItem> items = extractUiItems(doc);

            // ---- TAX 라인 추가 (중복 방지)
            BigDecimal tax = computeTaxFromEntities(doc); // 아래 헬퍼 추가
            if (tax != null && tax.compareTo(BigDecimal.ZERO) > 0 && !hasTaxItem(items)) {
                items.add(ReceiptResponse.UiItem.builder()
                        .name("TAX")
                        .amount(tax)
                        .build());
            }

            // ---- Total: 아이템 합계 (세금이 items에 포함되었으니 자동 포함)
            BigDecimal total = items.stream()
                    .map(ReceiptResponse.UiItem::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return ReceiptResponse.builder()
                    .currency(currencyFinal)
                    .totalAmount(total)
                    .receiptUrl(receiptUrl)
                    .items(items)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("영수증 파싱 실패: " + e.getMessage(), e);
        }
    }

    // ===================== Entities Helpers =====================

    private String getEntityText(Document doc, String type) {
        for (Document.Entity e : doc.getEntitiesList()) {
            if (type.equalsIgnoreCase(e.getType())) {
                String mt = nullToEmpty(e.getMentionText()).trim();
                if (!mt.isEmpty()) return mt;
                if (e.hasNormalizedValue()) {
                    String nv = nullToEmpty(e.getNormalizedValue().getText()).trim();
                    if (!nv.isEmpty()) return nv;
                }
            }
        }
        return null;
    }

    private String getEntityTextAny(Document doc, String... types) {
        for (String t : types) {
            String v = getEntityText(doc, t);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    // ===================== Currency (코드/심볼) =====================

    private String pickCurrencyCode(Document doc) {
        // currency 엔티티 우선
        for (Document.Entity e : doc.getEntitiesList()) {
            if ("currency".equalsIgnoreCase(e.getType()) && e.hasNormalizedValue()) {
                String t = nullToEmpty(e.getNormalizedValue().getText()).trim();
                if (!t.isBlank()) return t.toUpperCase();
                if (e.getNormalizedValue().hasMoneyValue()) {
                    String code = e.getNormalizedValue().getMoneyValue().getCurrencyCode();
                    if (code != null && !code.isBlank()) return code.toUpperCase();
                }
            }
        }
        // 금액성 엔티티/라인 아이템 속성
        Set<String> moneyFields = new HashSet<>(Arrays.asList(
                "total","grand_total","amount_due","subtotal","tax","tip","amount","line_total","unit_price"
        ));
        for (Document.Entity e : doc.getEntitiesList()) {
            String type = nullToEmpty(e.getType()).toLowerCase();
            if (moneyFields.contains(type)) {
                if (e.hasNormalizedValue() && e.getNormalizedValue().hasMoneyValue()) {
                    String code = e.getNormalizedValue().getMoneyValue().getCurrencyCode();
                    if (code != null && !code.isBlank()) return code.toUpperCase();
                }
            }
            if ("line_item".equalsIgnoreCase(e.getType())) {
                for (Document.Entity p : e.getPropertiesList()) {
                    String pt = nullToEmpty(p.getType()).toLowerCase();
                    if (pt.equals("amount") || pt.equals("line_total") || pt.equals("unit_price")) {
                        if (p.hasNormalizedValue() && p.getNormalizedValue().hasMoneyValue()) {
                            String code = p.getNormalizedValue().getMoneyValue().getCurrencyCode();
                            if (code != null && !code.isBlank()) return code.toUpperCase();
                        }
                    }
                }
            }
        }
        return null;
    }

    private static final Pattern ISO_CODE = Pattern.compile(
            "\\b(USD|AUD|CAD|NZD|SGD|HKD|TWD|JPY|EUR|GBP|CHF|SEK|NOK|DKK|PLN|CZK|HUF|TRY|ILS|INR|AED|SAR|THB|IDR|MYR|VND|ZAR|BRL|MXN|PHP|RUB|CNY|KRW)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private String detectCurrencyCodeFromRaw(String raw) {
        if (raw == null) return null;

        Matcher m = ISO_CODE.matcher(raw);
        if (m.find()) return m.group(1).toUpperCase();

        Map<String, String> prefixed = new LinkedHashMap<>();
        prefixed.put("NT$", "TWD"); prefixed.put("HK$", "HKD"); prefixed.put("A$", "AUD"); prefixed.put("AU$", "AUD");
        prefixed.put("S$", "SGD");  prefixed.put("CA$", "CAD"); prefixed.put("C$", "CAD");  prefixed.put("NZ$", "NZD");
        prefixed.put("R$", "BRL");  prefixed.put("MX$", "MXN"); prefixed.put("MEX$", "MXN");
        for (var e : prefixed.entrySet()) if (raw.contains(e.getKey())) return e.getValue();

        if (raw.contains("₩")) return "KRW";
        if (raw.contains("₹")) return "INR";
        if (raw.contains("฿")) return "THB";
        if (raw.contains("₱")) return "PHP";
        if (raw.contains("₽")) return "RUB";
        if (raw.contains("₺")) return "TRY";
        if (raw.contains("₪")) return "ILS";
        if (raw.contains("₫")) return "VND";
        if (raw.contains("CHF") || raw.contains(" Fr") || raw.contains("Fr.")) return "CHF";

        if (raw.contains("¥")) {
            if (raw.matches(".*(円|税込|東京都|大阪|日本|〒|\\bJP\\b|\\bJPN\\b).*")) return "JPY";
            if (raw.matches(".*(人民币|人民幣|元|中國|中国|\\bCN\\b|\\bCHN\\b).*")) return "CNY";
        }

        if (raw.contains("$")) return "USD";
        if (raw.contains("£")) return "GBP";
        if (raw.contains("€")) return "EUR";
        return null;
    }

    private String detectCurrencySymbolFromRaw(String raw) {
        if (raw == null) return null;
        if (raw.contains("₩")) return "₩";
        if (raw.contains("₹")) return "₹";
        if (raw.contains("฿")) return "฿";
        if (raw.contains("₱")) return "₱";
        if (raw.contains("₽")) return "₽";
        if (raw.contains("₺")) return "₺";
        if (raw.contains("₪")) return "₪";
        if (raw.contains("₫")) return "₫";
        String[] dollarPrefixes = {"NT$", "HK$", "A$", "AU$", "S$", "CA$", "C$", "NZ$", "R$", "MX$", "MEX$"};
        for (String p : dollarPrefixes) if (raw.contains(p)) return "$";
        if (raw.contains("€")) return "€";
        if (raw.contains("£")) return "£";
        if (raw.contains("¥")) return "¥";
        if (raw.contains("$")) return "$";
        return null;
    }

    private String symbolFromCurrencyCode(String code) {
        if (code == null) return null;
        return switch (code) {
            case "USD","AUD","CAD","NZD","SGD","HKD","TWD","MXN","BRL" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "JPY","CNY" -> "¥";
            case "KRW" -> "₩";
            case "INR" -> "₹";
            case "THB" -> "฿";
            case "PHP" -> "₱";
            case "RUB" -> "₽";
            case "TRY" -> "₺";
            case "ILS" -> "₪";
            case "VND" -> "₫";
            case "CHF" -> "Fr";
            default -> null;
        };
    }

    // ===================== UI Items =====================

    // 숫자 패턴(그룹화 쉼표/소수점 허용) — 라인 내 "마지막 숫자"를 금액으로 사용
    private static final Pattern NUM = Pattern.compile("\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?");

    /**
     * line_item 엔티티만 보고 단순 규칙으로 아이템을 만든다.
     * (문자+숫자) → 마지막 숫자를 amount, 앞부분을 name
     */
    private List<ReceiptResponse.UiItem> extractUiItems(Document doc) {
        List<ReceiptResponse.UiItem> out = new ArrayList<>();

        for (Document.Entity e : doc.getEntitiesList()) {
            if (!"line_item".equalsIgnoreCase(e.getType())) continue;

            String mt = e.getMentionText();
            if (mt == null || mt.isBlank()) continue;

            for (String raw : mt.split("\\R+")) {
                String line = raw.strip();
                if (line.isEmpty()) continue;

                boolean hasLetter = line.matches(".*\\p{L}.*");
                boolean hasDigit  = line.matches(".*\\d.*");

                // 문자 + 숫자 케이스만 처리
                if (hasLetter && hasDigit) {
                    Matcher m = NUM.matcher(line);
                    int lastStart = -1; String lastNum = null;
                    while (m.find()) { lastStart = m.start(); lastNum = m.group(); }
                    if (lastNum != null) {
                        String name = line.substring(0, lastStart).trim();
                        out.add(ReceiptResponse.UiItem.builder()
                                .name(cleanDescription(name))
                                .amount(toBig(lastNum))
                                .build());
                    }
                }
            }
        }
        return out;
    }


    // 엔티티의 normalized moneyValue 또는 normalized text에서 금액 추출
    private BigDecimal moneyFromEntity(Document.Entity e) {
        try {
            if (e.hasNormalizedValue()) {
                var nv = e.getNormalizedValue();
                if (nv.hasMoneyValue()) {
                    var mv = nv.getMoneyValue(); // google.type.Money
                    BigDecimal units = BigDecimal.valueOf(mv.getUnits());
                    BigDecimal nanos = BigDecimal.valueOf(mv.getNanos()).movePointLeft(9);
                    return units.add(nanos);
                }
                BigDecimal v = toBig(nv.getText());
                if (v != null) return v;
            }
        } catch (Exception ignore) {}
        return null;
    }

    // ===================== Utils =====================

    private BigDecimal toBig(String s) {
        try {
            if (s == null || s.isBlank()) return null;
            String t = s.trim();
            boolean neg = false;
            if (t.startsWith("(") && t.endsWith(")")) { neg = true; t = t.substring(1, t.length() - 1); }
            t = t.replaceAll("[^0-9.\\-]", "");
            if (t.isBlank()) return null;
            BigDecimal v = new BigDecimal(t);
            return neg ? v.negate() : v;
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanDescription(String s) {
        if (s == null) return null;
        s = s.replace("'", "").replace("`", "");
        s = s.replaceAll("^\\*+\\s*", "");
        s = s.replaceAll("\\bi\\s*MPERFECT\\b", "IMPERFECT");
        s = s.replace("STR AWB", "STRAWB");
        return s.replaceAll("\\s+", " ").trim();
    }

    private String nullToEmpty(String s) { return (s == null) ? "" : s; }

    private String firstNonEmpty(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private String safeClamp(String s, int limit) {
        if (s == null) return null;
        return (s.length() <= limit) ? s : s.substring(0, limit);
    }

    // 엔티티들에서 세금 금액 계산:
// 우선순위: total_tax_amount → (total_amount - net_amount) → 라인아이템 세금합
    private BigDecimal computeTaxFromEntities(Document doc) {
        BigDecimal entTax   = pickMoney(doc, "total_tax_amount");
        if (entTax != null) return entTax;

        BigDecimal entTotal = pickMoney(doc, "total_amount", "grand_total", "amount_due");
        BigDecimal entNet   = pickMoney(doc, "net_amount", "subtotal", "subtotal_amount");
        if (entTotal != null && entNet != null) {
            return entTotal.subtract(entNet);
        }

        return sumLineItemTaxes(doc); // 없으면 null
    }

    // 엔티티들에서 금액 하나 추출(우선순위): moneyValue → normalized.text → mentionText
    private BigDecimal pickMoney(Document doc, String... types) {
        for (String t : types) {
            for (Document.Entity e : doc.getEntitiesList()) {
                if (!t.equalsIgnoreCase(e.getType())) continue;

                if (e.hasNormalizedValue() && e.getNormalizedValue().hasMoneyValue()) {
                    var mv = e.getNormalizedValue().getMoneyValue();
                    BigDecimal units = BigDecimal.valueOf(mv.getUnits());
                    BigDecimal nanos = BigDecimal.valueOf(mv.getNanos()).movePointLeft(9);
                    return units.add(nanos);
                }
                if (e.hasNormalizedValue()) {
                    BigDecimal v = toBig(e.getNormalizedValue().getText());
                    if (v != null) return v;
                }
                BigDecimal v = toBig(e.getMentionText());
                if (v != null) return v;
            }
        }
        return null;
    }

    // 라인아이템 하위 속성 세금 합계(있을 때만)
// 커버 타입: tax, tax_amount, vat, gst, hst, pst
    private BigDecimal sumLineItemTaxes(Document doc) {
        BigDecimal sum = BigDecimal.ZERO;
        boolean hit = false;
        for (Document.Entity li : doc.getEntitiesList()) {
            if (!"line_item".equalsIgnoreCase(li.getType())) continue;
            for (Document.Entity p : li.getPropertiesList()) {
                String pt = (p.getType() == null) ? "" : p.getType().toLowerCase();
                if (!(pt.equals("tax") || pt.equals("tax_amount") || pt.equals("vat") ||
                        pt.equals("gst") || pt.equals("hst") || pt.equals("pst"))) continue;

                BigDecimal v = null;
                if (p.hasNormalizedValue() && p.getNormalizedValue().hasMoneyValue()) {
                    var mv = p.getNormalizedValue().getMoneyValue();
                    v = BigDecimal.valueOf(mv.getUnits())
                            .add(BigDecimal.valueOf(mv.getNanos()).movePointLeft(9));
                } else if (p.hasNormalizedValue()) {
                    v = toBig(p.getNormalizedValue().getText());
                }
                if (v == null) v = toBig(p.getMentionText());

                if (v != null) { sum = sum.add(v); hit = true; }
            }
        }
        return hit ? sum : null;
    }

    private boolean hasTaxItem(List<ReceiptResponse.UiItem> items) {
        if (items == null) return false;
        for (var it : items) {
            String n = (it.getName() == null) ? "" : it.getName().toLowerCase();
            if (n.matches(".*\\b(tax|sales\\s*tax|vat|gst|hst|pst|iva)\\b.*")) return true;
        }
        return false;
    }


}
