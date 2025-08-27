package com.snapsplit.backend.feature.receipt.service;

import com.snapsplit.backend.domain.receipt.entity.Receipt;
import com.snapsplit.backend.domain.receipt.repository.ReceiptRepository;
import com.snapsplit.backend.global.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptGarbageCollector {

    private final ReceiptRepository receiptRepository;
    private final S3Uploader s3Uploader;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanUnusedReceipts() {
        // 1) DB에 남아있는 영수증 URL을 Set으로 저장 (검색 성능 향상)
        Set<String> usedUrls = new HashSet<>(
                receiptRepository.findAll()
                        .stream()
                        .map(Receipt::getReceiptUrl)
                        .toList()
        );

        // 2) S3에 저장된 모든 영수증 파일 목록 가져오기
        List<String> allObjects = s3Uploader.listObjects("receipt-images");

        // 3) DB에 없는 파일만 수집
        List<String> urlsToDelete = allObjects.stream()
                .filter(url -> !usedUrls.contains(url))
                .toList();

        // 4) 배치 삭제
        for (String url : urlsToDelete) {
            try {
                s3Uploader.deleteByUrl(url);
                log.info("Deleted unused receipt: {}", url);
            } catch (Exception e) {
                // 개별 삭제 실패 로깅하고 계속 진행
                log.error("Failed to delete unused receipt: {}", url, e);
            }
        }
    }

}
