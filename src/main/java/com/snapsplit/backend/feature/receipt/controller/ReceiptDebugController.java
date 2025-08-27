package com.snapsplit.backend.feature.receipt.controller;

import com.snapsplit.backend.feature.receipt.dto.ReceiptDebugResponse;
import com.snapsplit.backend.feature.receipt.dto.ReceiptRequest;
import com.snapsplit.backend.feature.receipt.service.ReceiptDebug;
import com.snapsplit.backend.feature.receipt.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "영수증 OCR", description = "Document AI Expense Parser를 이용한 영수증 파싱")
@RestController
@RequiredArgsConstructor
@RequestMapping("/receipts")
public class ReceiptDebugController {

    private final ReceiptDebug receiptService;


    /** OCR 원본 디버그용: Document 전체 JSON + rawText + 엔티티 요약 */
    @Operation(summary = "영수증 원본 디버그용", description = "Document 전체 JSON + rawText + 엔티티 요약")
    @PostMapping("/debug")
    public ResponseEntity<ReceiptDebugResponse> debug(
            @ModelAttribute ReceiptRequest request,
            @RequestParam(name = "pretty", defaultValue = "false") boolean pretty,
            @RequestParam(name = "entitiesLimit", defaultValue = "200") int entitiesLimit
    ) {
        return ResponseEntity.ok(receiptService.debug(request, pretty, entitiesLimit));
    }
}
