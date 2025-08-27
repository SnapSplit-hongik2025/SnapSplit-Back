package com.snapsplit.backend.global.s3.dto;

import lombok.Getter;

@Getter
public class S3UploadResult {
    private final String fileKey;
    private final String fileUrl;

    public S3UploadResult(String fileKey, String fileUrl) {
        this.fileKey = fileKey;
        this.fileUrl = fileUrl;
    }
}