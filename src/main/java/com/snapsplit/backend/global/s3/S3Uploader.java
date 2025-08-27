package com.snapsplit.backend.global.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class S3Uploader {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file, String dirName) throws IOException {
        String fileName = dirName + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(bucket, fileName, file.getInputStream(), metadata);

        return amazonS3.getUrl(bucket, fileName).toString(); // URL 반환
    }

    // 객체 리스트 가져오기 (페이징 포함)
    public List<String> listObjects(String dir) {
        List<String> urls = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(bucket)
                    .withPrefix(dir)
                    .withContinuationToken(continuationToken);

            ListObjectsV2Result result = amazonS3.listObjectsV2(request);

            result.getObjectSummaries().forEach(s ->
                    urls.add(amazonS3.getUrl(bucket, s.getKey()).toString())
            );

            continuationToken = result.getNextContinuationToken();
        } while (continuationToken != null);

        return urls;
    }


    // 객체 삭제
    public void delete(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        amazonS3.deleteObject(bucket, key);
    }

    private String extractKeyFromUrl(String fileUrl) {
        try {
            URI uri = URI.create(fileUrl);
            String path = uri.getPath(); // "/bucket/key" or "/key"
            if (path.startsWith("/")) path = path.substring(1);

            // path-style 주소인 경우 bucket/ 제거
            if (path.startsWith(bucket + "/")) {
                path = path.substring(bucket.length() + 1);
            }

            // 퍼센트 인코딩 해제
            return URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 S3 URL: " + fileUrl, e);
        }
    }
}
