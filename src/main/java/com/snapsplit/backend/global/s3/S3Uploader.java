package com.snapsplit.backend.global.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    // 객체 리스트 가져오기
    public List<String> listObjects(String dir) {
        ListObjectsV2Result result = amazonS3.listObjectsV2(bucket, dir);
        return result.getObjectSummaries().stream()
                .map(s -> String.format("https://%s.s3.%s.amazonaws.com/%s",
                        bucket,
                        amazonS3.getRegionName(),
                        s.getKey()))
                .toList();
    }

    // 객체 삭제
    public void delete(String fileUrl) {
        String key = extractKeyFromUrl(fileUrl);
        amazonS3.deleteObject(bucket, key);
    }

    private String extractKeyFromUrl(String fileUrl) {
        return fileUrl.substring(fileUrl.indexOf(".com/") + 5);
    }
}
