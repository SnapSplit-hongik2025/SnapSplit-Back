package com.snapsplit.backend.feature.receipt.service;

import com.snapsplit.backend.domain.receipt.entity.Receipt;
import com.snapsplit.backend.domain.receipt.repository.ReceiptRepository;
import com.snapsplit.backend.global.s3.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceiptGarbageCollector {

    private final ReceiptRepository receiptRepository;
    private final S3Uploader s3Uploader;

    // 매일 새벽 3시에 실행
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanUnusedReceipts() {
        // 1) DB에 남아있는 영수증 URL 전부 가져오기
        List<String> usedUrls = receiptRepository.findAll()
                .stream()
                .map(Receipt::getReceiptUrl)
                .toList();

        // 2) S3에 저장된 모든 영수증 파일 목록 가져오기
        List<String> allObjects = s3Uploader.listObjects("receipt-images");

        // 3) DB에 없는 파일만 삭제
        allObjects.stream()
                .filter(url -> !usedUrls.contains(url))
                .forEach(s3Uploader::delete);
    }
}
