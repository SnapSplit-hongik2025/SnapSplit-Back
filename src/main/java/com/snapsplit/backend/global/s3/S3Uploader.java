package com.snapsplit.backend.global.s3;

import java.io.IOException;
import com.snapsplit.backend.global.s3.dto.S3UploadResult;
import com.snapsplit.backend.config.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;


@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    public S3UploadResult upload(MultipartFile multipartFile, String dirName) throws IOException {
        // 1. S3 버킷 이름 가져오기
        String bucket = awsProperties.getS3().getBucket();

        // 2. 파일 이름 생성 (중복 방지를 위해 UUID 사용)
        String originalFilename = multipartFile.getOriginalFilename();
        String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;

        // 3. S3에 저장될 파일의 전체 경로 (키) 생성
        String key = dirName + "/" + uniqueFilename;

        // 4. S3 업로드 요청 객체 생성 (V2 방식)
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(multipartFile.getContentType())
                .contentLength(multipartFile.getSize())
                .build();

        // 5. 파일 업로드 실행
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

        // 6. 업로드된 파일의 URL 반환
        String url = s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(key)).toExternalForm();
        return new S3UploadResult(key, url);
    }

    public S3Client getS3Client() {
        return this.s3Client;
    }

}
