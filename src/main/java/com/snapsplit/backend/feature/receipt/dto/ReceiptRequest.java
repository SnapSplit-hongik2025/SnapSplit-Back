package com.snapsplit.backend.feature.receipt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter @Setter
public class ReceiptRequest {

    @NotNull(message = "영수증 이미지 파일은 필수입니다.")
    private MultipartFile file;
}
