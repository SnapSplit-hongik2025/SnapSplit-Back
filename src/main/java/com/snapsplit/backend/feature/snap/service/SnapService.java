package com.snapsplit.backend.feature.snap.service;

import com.snapsplit.backend.config.properties.AwsProperties;
import com.snapsplit.backend.domain.album.entity.Album;
import com.snapsplit.backend.domain.album.repository.AlbumRepository;
import com.snapsplit.backend.domain.photo.entity.Photo;
import com.snapsplit.backend.domain.photo.repository.PhotoRepository;
import com.snapsplit.backend.domain.phototag.entity.PhotoTag;
import com.snapsplit.backend.domain.phototag.repository.PhotoTagRepository;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.tripmember.entity.MemberType;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.feature.snap.dto.UploadPhotoResponse;
import com.snapsplit.backend.global.s3.S3Uploader;
import com.snapsplit.backend.global.s3.dto.S3UploadResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapService {

    private final PhotoRepository photoRepository;
    private final PhotoTagRepository photoTagRepository;
    private final UserRepository userRepository;
    private final TripMemberRepository tripMemberRepository;
    private final AlbumRepository albumRepository; // Trip에 연결된 Album을 찾기 위함
    private final S3Uploader s3Uploader;
    private final RekognitionClient rekognitionClient;
    private final AwsProperties awsProperties;

    @Transactional
    public List<UploadPhotoResponse> uploadAndTagPhotos(Long tripId, List<MultipartFile> images) {
        // 1. tripId로 Album 찾기
        Album album = albumRepository.findByTripId(tripId)
                .orElseThrow(() -> new EntityNotFoundException("해당 여행의 앨범을 찾을 수 없습니다."));

        // 검색할 최대 얼굴 수 설정 (멤버가 없으면 기본값 10으로 설정)
        int memberCount = tripMemberRepository.countByTrip_IdAndMemberType(tripId, MemberType.USER);
        int maxFacesToDetect = (memberCount > 0) ? memberCount : 10;
        log.info("Attempting to find max {} faces for tripId {}.", maxFacesToDetect, tripId);

        List<UploadPhotoResponse> responses = new ArrayList<>();

        for (MultipartFile image : images) {
            try {
                // 2. S3에 이미지 업로드
                S3UploadResult uploadResult = s3Uploader.upload(image, "photos");
                String s3Url = uploadResult.getFileUrl();
                String s3Key = uploadResult.getFileKey();

                // 3. Photo 엔티티 생성 및 DB 저장
                Photo photo = Photo.builder().album(album).s3Url(s3Url).build();
                Photo savedPhoto = photoRepository.save(photo);

                // --- [디버깅 로그 추가 시작] ---
                log.info("===== [Photo: {}] 분석 시작 =====", image.getOriginalFilename());
                log.info("Rekognition 요청: maxFaces = {}", maxFacesToDetect);

                // 4. Rekognition으로 얼굴 검색
                SearchFacesByImageRequest searchRequest = SearchFacesByImageRequest.builder()
                        .collectionId(awsProperties.getRekognition().getCollectionId())
                        .image(Image.builder().s3Object(
                                S3Object.builder()
                                        .bucket(awsProperties.getS3().getBucket())
                                        .name(s3Key)
                                        .build()
                        ).build())
                        .faceMatchThreshold(90F)
                        .maxFaces(maxFacesToDetect)
                        .build();
                SearchFacesByImageResponse searchResponse = rekognitionClient.searchFacesByImage(searchRequest);

                // Rekognition 응답을 직접 확인하는 가장 중요한 로그
                log.info("Rekognition 응답: 찾은 얼굴 수 = {}", searchResponse.faceMatches().size());
                searchResponse.faceMatches().forEach(faceMatch -> {
                    log.info(" -> 매칭된 FaceId: {}, 유사도: {}", faceMatch.face().faceId(), faceMatch.similarity());
                });
                // --- [디버깅 로그 추가 끝] ---

                // 5. 매칭된 얼굴로 PhotoTag 생성 및 저장
                List<UploadPhotoResponse.TaggedUser> taggedUsers = searchResponse.faceMatches().stream()
                        .flatMap(faceMatch -> userRepository.findByAwsFaceId(faceMatch.face().faceId()).stream())
                        .map(user -> {
                            photoTagRepository.save(new PhotoTag(savedPhoto, user));
                            return UploadPhotoResponse.TaggedUser.builder()
                                    .userId(user.getId()).userName(user.getName()).build();
                        })
                        .collect(Collectors.toList());

                // 6. 최종 응답 객체 생성
                responses.add(UploadPhotoResponse.builder()
                        .photoId(savedPhoto.getId()).photoUrl(s3Url).taggedUsers(taggedUsers).build());

            } catch (IOException e) {
                //log.error("파일 처리 중 오류 발생: {}", image.getOriginalFilename(), e);
            }
        }
        return responses;
    }

    // Todo: 사진 삭제, 목록 조회, 폴더 조회 등 다른 서비스 메소드 구현
}