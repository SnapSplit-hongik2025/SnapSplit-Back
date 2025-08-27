package com.snapsplit.backend.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.DocumentProcessorServiceSettings;
import com.google.cloud.documentai.v1.ProcessorName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.util.List;

@Configuration
public class DocAiConfig {

    @Value("${gcp.docai.credentials-path}")
    private String credentialsPath;

    @Value("${gcp.docai.project-id}")
    private String projectId;

    @Value("${gcp.docai.location}")
    private String location;

    @Value("${gcp.docai.processor-id}")
    private String processorId;

    @Bean
    public DocumentProcessorServiceClient documentProcessorServiceClient() throws Exception {
        // 1) 자격 증명 로드 + 스코프 강제
        GoogleCredentials creds;
        try (FileInputStream fis = new FileInputStream(credentialsPath)) {
            creds = GoogleCredentials.fromStream(fis)
                    .createScoped(DocumentProcessorServiceSettings.getDefaultServiceScopes());
        }

        // 2) 리전 엔드포인트 고정 (us/eu/asia)
        DocumentProcessorServiceSettings settings = DocumentProcessorServiceSettings.newBuilder()
                .setEndpoint(String.format("%s-documentai.googleapis.com:443", location))
                .setCredentialsProvider(FixedCredentialsProvider.create(creds))
                .build();

        // 3) 디버그 로그 (실행 후 콘솔에서 필히 확인)
        String procName = ProcessorName.of(projectId, location, processorId).toString();
        System.out.println("[DOC-AI] credentials-path = " + credentialsPath);
        System.out.println("[DOC-AI] processor = " + procName);
        System.out.println("[DOC-AI] endpoint  = " + settings.getEndpoint());
        // client email은 creds에서 직접 노출 메서드가 없어 키 JSON 파일명으로만 추정됨 (보안상 노출 지양)

        return DocumentProcessorServiceClient.create(settings);
    }
}
