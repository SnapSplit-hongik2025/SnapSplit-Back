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
        validateImage(faceImage);
        User user = getCurrentUser();

        // 1. 이미 등록된 얼굴이 있는지 확인하고, 있다면 에러 발생
        if (user.getAwsFaceId() != null && !user.getAwsFaceId().isEmpty()) {
            throw new IllegalStateException("이미 등록된 얼굴이 있습니다. 수정을 원하시면 변경하기 기능을 이용해주세요.");
        }

        // 2. 새로운 얼굴 정보 등록 진행
        registerNewFaceData(user, faceImage);
    }

    @Transactional
    public void updateFace(MultipartFile faceImage) throws IOException {
        validateImage(faceImage);
        User user = getCurrentUser();

        // 1. 등록된 얼굴 정보가 있는지 확인하고, 없다면 에러 발생
        if (user.getAwsFaceId() == null || user.getAwsFaceId().isEmpty()) {
            throw new EntityNotFoundException("수정할 기존 얼굴 정보가 없습니다. 먼저 얼굴을 등록해주세요.");
        }

        // 2. 기존 얼굴 정보(Rekognition, S3)를 먼저 삭제
        deleteExistingFaceData(user);

        // 3. 새로운 얼굴 정보 등록 진행
        registerNewFaceData(user, faceImage);
    }

    // 새로운 얼굴을 S3와 Rekognition에 등록하고 DB에 저장하는 공통 로직
    private void registerNewFaceData(User user, MultipartFile faceImage) throws IOException {
        // S3에 새로운 얼굴 이미지 업로드
        S3UploadResult uploadResult = s3Uploader.upload(faceImage, "faces");

        // Rekognition에 새로운 얼굴 등록 요청
        IndexFacesRequest request = IndexFacesRequest.builder()
                .collectionId(awsProperties.getRekognition().getCollectionId())
                .image(Image.builder().s3Object(
                        S3Object.builder()
                                .bucket(awsProperties.getS3().getBucket())
                                .name(uploadResult.getFileKey())
                                .build()
                ).build())
                .maxFaces(1)
                .qualityFilter(QualityFilter.AUTO)
                .build();

        IndexFacesResponse response = rekognitionClient.indexFaces(request);

        // 응답 확인 및 새로운 FaceId, ImageUrl 저장
        if (response.faceRecords().isEmpty()) {
            s3Uploader.deleteByKey(uploadResult.getFileKey());
            throw new IllegalArgumentException("사진에서 얼굴을 찾을 수 없거나, 이미지 품질이 너무 낮습니다.");
        }
        String newFaceId = response.faceRecords().get(0).face().faceId();
        user.setAwsFaceId(newFaceId);
        user.setFaceImageUrl(uploadResult.getFileUrl());
    }



    // 사용자의 기존 얼굴 데이터를 삭제하는 공통 로직
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

    // 현재 로그인한 사용자 정보를 가져오는 공통 로직
    private User getCurrentUser() {
        Long currentUserId = securityUtil.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException("해당 유저를 찾을 수 없습니다. ID: " + currentUserId));
    }

    // 이미지 파일의 유효성을 검사하는 공통 로직
    private void validateImage(MultipartFile faceImage) {
        if (faceImage == null || faceImage.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }
        long fileSize = faceImage.getSize();
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (fileSize > maxSize) {
            throw new IllegalArgumentException("이미지 파일 용량은 5MB를 초과할 수 없습니다.");
        }
    }
}