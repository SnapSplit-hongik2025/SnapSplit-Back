package com.snapsplit.backend.feature.face.service;

import com.snapsplit.backend.config.properties.AwsProperties;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.global.s3.S3Uploader;
import com.snapsplit.backend.global.s3.dto.S3UploadResult;
import com.snapsplit.backend.global.security.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class FaceService {

    private final UserRepository userRepository;
    private final RekognitionClient rekognitionClient;
    private final AwsProperties awsProperties;
    private final SecurityUtil securityUtil;
    private final S3Uploader s3Uploader;

    @Transactional
    public void registerFace(MultipartFile faceImage) throws IOException {

        long fileSize = faceImage.getSize();
        long maxSize = 5 * 1024 * 1024; // 5MB

        if (fileSize > maxSize) {
            throw new IllegalArgumentException("이미지 파일 용량은 5MB를 초과할 수 없습니다.");
        }

        // 1. 사용자 정보 조회
        Long currentUserId = securityUtil.getCurrentUserId();

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저를 찾을 수 없습니다. ID: " + currentUserId));

        // 2. 이미 등록된 얼굴이 있다면 기존 정보(Rekognition, S3)를 먼저 삭제
        if (user.getAwsFaceId() != null && !user.getAwsFaceId().isEmpty()) {
            deleteExistingFaceData(user);
        }

        // 3. S3에 얼굴 이미지 업로드
        S3UploadResult uploadResult = s3Uploader.upload(faceImage, "faces");

        // 4. Rekognition에 얼굴 등록 요청
        IndexFacesRequest request = IndexFacesRequest.builder()
                .collectionId(awsProperties.getRekognition().getCollectionId())
                .image(Image.builder().s3Object(
                        S3Object.builder()
                                .bucket(awsProperties.getS3().getBucket())
                                .name(uploadResult.getFileKey()) // S3 키 사용
                                .build()
                ).build())
                .maxFaces(1)
                .qualityFilter(QualityFilter.AUTO)
                .build();

        IndexFacesResponse response = rekognitionClient.indexFaces(request);

        // 5. 응답 확인 및 FaceId 저장
        if (response.faceRecords().isEmpty()) {
            // Rekognition 등록 실패 시, 방금 올린 S3 파일도 다시 삭제
            s3Uploader.deleteByKey(uploadResult.getFileKey());
            throw new IllegalArgumentException("사진에서 얼굴을 찾을 수 없거나, 이미지 품질이 너무 낮습니다.");
        }
        String newFaceId = response.faceRecords().get(0).face().faceId();
        user.setAwsFaceId(newFaceId);
        user.setFaceImageUrl(uploadResult.getFileUrl());
    }

    @Transactional
    public void deleteFace() {
        Long currentUserId = securityUtil.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저를 찾을 수 없습니다. ID: " + currentUserId));

        deleteExistingFaceData(user);
    }

    // 사용자의 기존 얼굴 데이터를 Rekognition과 S3에서 모두 삭제하는 메소드
    private void deleteExistingFaceData(User user) {
        String faceId = user.getAwsFaceId();
        String faceImageUrl = user.getFaceImageUrl();

        // 1. 등록된 얼굴 정보(faceId)가 있는지 확인하고 Rekognition에서 삭제
        if (faceId != null && !faceId.isEmpty()) {
            DeleteFacesRequest request = DeleteFacesRequest.builder()
                    .collectionId(awsProperties.getRekognition().getCollectionId())
                    .faceIds(faceId)
                    .build();
            rekognitionClient.deleteFaces(request);
            user.setAwsFaceId(null);
        }

        // 2. 등록된 얼굴 이미지(URL)가 있는지 확인하고 S3에서 삭제
        if (faceImageUrl != null && !faceImageUrl.isEmpty()) {
            s3Uploader.deleteByUrl(faceImageUrl);
            user.setFaceImageUrl(null);
        }
    }


}