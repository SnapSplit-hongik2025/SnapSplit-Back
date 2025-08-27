package com.snapsplit.backend.feature.face.service;

import com.snapsplit.backend.config.properties.AwsProperties;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.global.security.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.core.SdkBytes;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class FaceService {

    private final UserRepository userRepository;
    private final RekognitionClient rekognitionClient;
    private final AwsProperties awsProperties;
    private final SecurityUtil securityUtil;

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

        // 2. 이미 등록된 얼굴이 있는지 확인
        if (user.getAwsFaceId() != null && !user.getAwsFaceId().isEmpty()) {
            throw new IllegalStateException("이미 등록된 얼굴이 있습니다. 삭제 후 다시 시도해주세요.");
        }

        // 3. Rekognition에 얼굴 등록 요청
        IndexFacesRequest request = IndexFacesRequest.builder()
                .collectionId(awsProperties.getRekognition().getCollectionId())
                .image(Image.builder().bytes(SdkBytes.fromByteArray(faceImage.getBytes())).build())
                .maxFaces(1) // 사진에서 가장 큰 얼굴 하나만 등록
                .qualityFilter(QualityFilter.AUTO)
                .build();

        IndexFacesResponse response = rekognitionClient.indexFaces(request);

        // 4. 응답 확인 및 FaceId 저장
        if (response.faceRecords().isEmpty()) {
            throw new IllegalArgumentException("사진에서 얼굴을 찾을 수 없거나, 이미지 품질이 너무 낮습니다.");
        }
        String newFaceId = response.faceRecords().get(0).face().faceId();
        user.setAwsFaceId(newFaceId);
    }

    @Transactional
    public void deleteFace() {
        // 1. 사용자 정보 조회
        Long currentUserId = securityUtil.getCurrentUserId();

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저를 찾을 수 없습니다. ID: " + currentUserId));

        String faceId = user.getAwsFaceId();

        // 2. 등록된 얼굴 정보가 있는지 확인
        if (faceId == null || faceId.isEmpty()) {
            throw new IllegalStateException("등록된 얼굴 정보가 없습니다.");
        }

        // 3. Rekognition에 얼굴 삭제 요청
        DeleteFacesRequest request = DeleteFacesRequest.builder()
                .collectionId(awsProperties.getRekognition().getCollectionId())
                .faceIds(faceId)
                .build();

        rekognitionClient.deleteFaces(request);

        // 4. DB에서 FaceId 정보 삭제
        user.setAwsFaceId(null);

        // Todo : 얼굴 정보 삭제 시, 이전에 해당 얼굴로 태그되었던 PhotoTag들 처리 추가하기
    }
}