package com.snapsplit.backend.global.s3;

import com.snapsplit.backend.config.properties.AwsProperties;
import com.snapsplit.backend.global.s3.dto.S3UploadResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    /**
     * 파일을 S3에 업로드합니다. (snap 브랜치의 최신 코드)
     * @param multipartFile 업로드할 파일
     * @param dirName S3 버킷 내의 디렉토리 이름
     * @return 업로드 결과 (파일 키, URL)
     * @throws IOException 파일 처리 중 예외 발생 시
     */
    public S3UploadResult upload(MultipartFile multipartFile, String dirName) throws IOException {
        String bucket = awsProperties.getS3().getBucket();
        String originalFilename = multipartFile.getOriginalFilename();
        String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
        String key = dirName + "/" + uniqueFilename;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(multipartFile.getContentType())
                .contentLength(multipartFile.getSize())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

        String url = s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(key)).toExternalForm();
        return new S3UploadResult(key, url);
    }

    /**
     * S3에서 파일을 파일 키(key)로 삭제합니다. (snap 브랜치의 최신 코드)
     * @param fileKey 삭제할 파일의 키 (예: "images/uuid_filename.jpg")
     */
    public void deleteByKey(String fileKey) {
        log.info("S3에서 파일 삭제 시도 (by key): {}", fileKey);
        String bucket = awsProperties.getS3().getBucket();

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("S3에서 파일 삭제 완료 (by key): {}", fileKey);
    }

    /**
     * S3에서 파일을 전체 URL로 삭제합니다. (develop 브랜치의 기능을 최신 코드로 구현)
     * @param fileUrl 삭제할 파일의 전체 URL
     */
    public void deleteByUrl(String fileUrl) {
        log.info("S3에서 파일 삭제 시도 (by URL): {}", fileUrl);
        String key = extractKeyFromUrl(fileUrl);
        deleteByKey(key); // 이름이 변경된 key 기반 삭제 메서드를 재사용
    }

    /**
     * 특정 디렉토리의 모든 객체 URL 목록을 가져옵니다. (develop 브랜치의 기능을 최신 코드로 구현)
     * @param dirName 조회할 디렉토리 이름
     * @return 해당 디렉토리의 모든 파일 URL 리스트
     */
    public List<String> listObjects(String dirName) {
        String bucket = awsProperties.getS3().getBucket();

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(dirName)
                .build();

        // listObjectsV2Paginator를 사용하면 결과가 많아도 자동으로 페이징 처리해줍니다.
        ListObjectsV2Iterable responseIterable = s3Client.listObjectsV2Paginator(request);

        // ★★★ 스트림 처리 결과를 바로 return 하도록 수정하여 경고 해결 ★★★
        return responseIterable.stream()
                .flatMap(response -> response.contents().stream()) // List<S3Object>를 Stream<S3Object>으로 변환
                .map(s3Object -> {
                    String key = s3Object.key();
                    return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(key)).toExternalForm();
                })
                .collect(Collectors.toList());
    }

    /**
     * 전체 S3 URL에서 파일 키(key)를 추출합니다. (develop 브랜치의 기능)
     * @param fileUrl 전체 파일 URL
     * @return 파일 키
     */
    private String extractKeyFromUrl(String fileUrl) {
        try {
            String bucket = awsProperties.getS3().getBucket();
            URI uri = URI.create(fileUrl);
            String path = uri.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            // path-style 주소인 경우 (예: /bucket-name/images/...)
            if (path.startsWith(bucket + "/")) {
                return path.substring(bucket.length() + 1);
            }

            // virtual-hosted-style 주소인 경우 (예: /images/...)
            return path;

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 S3 URL: " + fileUrl, e);
        }
    }
}