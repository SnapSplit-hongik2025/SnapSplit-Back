package com.snapsplit.backend.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "cloud.aws")
public class AwsProperties {

    private final S3 s3 = new S3();
    private final Rekognition rekognition = new Rekognition();

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
    }

    @Getter
    @Setter
    public static class Rekognition {
        private String collectionId;
    }
}