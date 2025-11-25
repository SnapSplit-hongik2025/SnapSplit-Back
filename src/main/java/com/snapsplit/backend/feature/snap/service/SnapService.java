package com.snapsplit.backend.feature.snap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import com.snapsplit.backend.config.properties.AwsProperties;
import com.snapsplit.backend.domain.album.entity.Album;
import com.snapsplit.backend.domain.album.repository.AlbumRepository;
import com.snapsplit.backend.domain.photo.entity.Photo;
import com.snapsplit.backend.domain.photo.repository.PhotoRepository;
import com.snapsplit.backend.domain.phototag.entity.PhotoTag;
import com.snapsplit.backend.domain.phototag.repository.PhotoTagRepository;
import com.snapsplit.backend.domain.tripmember.entity.MemberType;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.feature.snap.dto.*;
import com.snapsplit.backend.global.s3.S3Uploader;
import com.snapsplit.backend.global.s3.dto.S3UploadResult;
import com.snapsplit.backend.global.security.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapService {

    private static final int PAGE_SIZE = 15;
    private final PhotoRepository photoRepository;
    private final PhotoTagRepository photoTagRepository;
    private final UserRepository userRepository;
    private final TripMemberRepository tripMemberRepository;
    private final AlbumRepository albumRepository;
    private final S3Uploader s3Uploader;
    private final RekognitionClient rekognitionClient;
    private final AwsProperties awsProperties;
    private final ExifService exifService;
    private final SecurityUtil securityUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    @Value("${snap.readiness-check-enabled}")
    private boolean readinessCheckEnabled;

    // Redis에 저장될 공용 데이터 전체를 감싸는 DTO
    @Getter
    @Builder
    private static class CachedSnapStatus {
        private boolean allMembersRegistered;
        private List<CachedMemberStatus> members;
    }

    // Redis에 저장될 멤버 한 명의 공용 정보 DTO
    @Getter
    @Builder
    private static class CachedMemberStatus {
        private Long userId;
        private String name;
        private String profileImageUrl;
        private boolean hasFaceData;
    }

    // 여행 사진 업로드 및 자동 태깅
    @Transactional
    public List<UploadPhotoResponse> uploadAndTagPhotos(Long tripId, List<MultipartFile> images) {

        log.info("[SERVICE] uploadAndTagPhotos 진입 tripId={}, images={}",
                tripId, images != null ? images.size() : null);

        Album album = albumRepository.findByTripId(tripId)
                .orElseThrow(() -> new EntityNotFoundException("해당 여행의 앨범을 찾을 수 없습니다."));

        int memberCount = tripMemberRepository.countByTrip_IdAndMemberType(tripId, MemberType.USER);
        int maxFacesToDetect = (memberCount > 0) ? memberCount : 10;
        //log.info("Attempting to find max {} faces for tripId {}.", maxFacesToDetect, tripId);

        return images.stream()
                .peek(img -> log.info("[SERVICE] processSingleImage 호출 시도 filename={}, size={}",
                        img.getOriginalFilename(), img.getSize()))
                .map(image -> processSingleImage(album, image, maxFacesToDetect))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }


    // 단일 이미지를 처리하는 전체 과정을 담당
    private UploadPhotoResponse processSingleImage(Album album, MultipartFile imageFile, int maxFacesToDetect) {
        try {

            ExifService.ExifData exif = exifService.extract(imageFile).orElse(null);
            LocalDateTime takenAt = null;

            if (exif != null) {
                if (exif.getTakenAtLocal() != null) {
                    takenAt = LocalDateTime.ofInstant(
                            exif.getTakenAtLocal().toInstant(),
                            ZoneId.systemDefault()
                    );
                }
            }

            // 1-1. S3 업로드
            S3UploadResult uploadResult = s3Uploader.upload(imageFile, "photos");
            // 1-2. Photo 저장 (EXIF 메타데이터 포함)
            Photo savedPhoto = photoRepository.save(
                    Photo.builder()
                            .album(album)
                            .s3Url(uploadResult.getFileUrl())
                            .photoDt(takenAt)
                            .build()
            );
            log.info("===== [Photo: {}] 분석 시작 =====", imageFile.getOriginalFilename());

            // 2. 원본 이미지에서 모든 얼굴 위치 검출
            List<FaceDetail> faceDetails = detectFacesFromS3Image(uploadResult.getFileKey());
            if (faceDetails.isEmpty()) {
                log.info("DetectFaces: 사진에서 얼굴을 검출하지 못했습니다.");
                return createUploadResponse(savedPhoto, Collections.emptyList());
            }

            // 3. 검출된 각 얼굴을 검색하여 태그 생성
            List<UploadPhotoResponse.TaggedUser> taggedUsers = identifyAndTagFaces(savedPhoto, uploadResult.getFileKey(), faceDetails, maxFacesToDetect);

            log.info("===== [Photo: {}] 분석 완료, 최종 태그된 유저 수 = {} =====", imageFile.getOriginalFilename(), taggedUsers.size());
            return createUploadResponse(savedPhoto, taggedUsers);

        } catch (IOException e) {
            log.error("파일 처리 중 오류 발생: {}", imageFile.getOriginalFilename(), e);
            return null;
        }
    }

    // S3에 저장된 이미지에서 모든 얼굴의 위치를 검출
    private List<FaceDetail> detectFacesFromS3Image(String s3Key) {
        DetectFacesRequest detectRequest = DetectFacesRequest.builder()
                .image(Image.builder().s3Object(
                        S3Object.builder()
                                .bucket(awsProperties.getS3().getBucket())
                                .name(s3Key)
                                .build()
                ).build())
                .attributes(Attribute.DEFAULT)
                .build();
        DetectFacesResponse detectResp = rekognitionClient.detectFaces(detectRequest);
        log.info("DetectFaces 응답: 검출한 얼굴 수 = {}", detectResp.faceDetails().size());
        return detectResp.faceDetails();
    }


     // 검출된 얼굴들을 하나씩 잘라내어 검색하고, 일치하는 사용자를 태그
    private List<UploadPhotoResponse.TaggedUser> identifyAndTagFaces(Photo photo, String s3Key, List<FaceDetail> faceDetails, int maxFacesToDetect) throws IOException {
        BufferedImage originalImage = downloadImageFromS3(s3Key);
        if (originalImage == null) return Collections.emptyList();

        Set<Long> taggedUserIds = new HashSet<>(); // 한 사진에 동일 인물 중복 태깅 방지

        return faceDetails.stream()
                .map(faceDetail -> cropAndSearchFace(originalImage, faceDetail, maxFacesToDetect))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(List::stream)
                .map(faceMatch -> userRepository.findByAwsFaceId(faceMatch.face().faceId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(user -> taggedUserIds.add(user.getId())) // 중복되지 않은 경우에만 통과
                .map(user -> {
                    photoTagRepository.save(new PhotoTag(photo, user));
                    return UploadPhotoResponse.TaggedUser.builder()
                            .userId(user.getId()).userName(user.getName()).build();
                })
                .collect(Collectors.toList());
    }

     //단일 얼굴을 잘라내어 Rekognition으로 검색
    private Optional<List<FaceMatch>> cropAndSearchFace(BufferedImage originalImage, FaceDetail faceDetail, int maxFacesToDetect) {
        try {
            BoundingBox bb = faceDetail.boundingBox();
            int W = originalImage.getWidth();
            int H = originalImage.getHeight();

            int x = Math.max(0, Math.round(bb.left() * W));
            int y = Math.max(0, Math.round(bb.top() * H));
            int w = Math.max(1, Math.min(Math.round(bb.width() * W), W - x));
            int h = Math.max(1, Math.min(Math.round(bb.height() * H), H - y));

            BufferedImage croppedImage = originalImage.getSubimage(x, y, w, h);

            if (croppedImage.getWidth() < 40 || croppedImage.getHeight() < 40) {
                log.warn("검출된 얼굴이 너무 작아서 건너뜁니다. (Size: {}x{})", croppedImage.getWidth(), croppedImage.getHeight());
                return Optional.empty();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(croppedImage, "jpg", baos);
            byte[] faceBytes = baos.toByteArray();

            SearchFacesByImageRequest searchRequest = SearchFacesByImageRequest.builder()
                    .collectionId(awsProperties.getRekognition().getCollectionId())
                    .image(Image.builder().bytes(SdkBytes.fromByteArray(faceBytes)).build())
                    .faceMatchThreshold(80F)
                    .maxFaces(1) // 잘라낸 얼굴 하나에 대해 가장 일치하는 사람 1명만 찾으면 됨
                    .build();

            SearchFacesByImageResponse searchResponse = rekognitionClient.searchFacesByImage(searchRequest);
            log.info("SearchFaces 응답(얼굴 1개): 찾은 매칭 수 = {}", searchResponse.faceMatches().size());
            return Optional.of(searchResponse.faceMatches());

        } catch (IOException | RasterFormatException | InvalidParameterException e) {
            log.warn("개별 얼굴 처리 중 오류 발생: {}", e.getMessage());
            return Optional.empty();
        }
    }


     // S3에서 이미지를 다운로드하여 BufferedImage 객체로 반환
    private BufferedImage downloadImageFromS3(String s3Key) throws IOException {
        try (var s3obj = s3Uploader.getS3Client().getObject(
                GetObjectRequest.builder()
                        .bucket(awsProperties.getS3().getBucket())
                        .key(s3Key)
                        .build())) {
            return ImageIO.read(s3obj);
        } catch (IOException e) {
            log.error("S3에서 이미지를 읽어오지 못했습니다. Key: {}", s3Key, e);
            throw e;
        }
    }

    //최종 응답 DTO 생성
    private UploadPhotoResponse createUploadResponse(Photo photo, List<UploadPhotoResponse.TaggedUser> taggedUsers) {
        return UploadPhotoResponse.builder()
                .photoId(photo.getId())
                .photoUrl(photo.getS3Url())
                .taggedUsers(taggedUsers)
                .takenAt(photo.getPhotoDt())
                .latitude(photo.getLatitude())
                .longitude(photo.getLongitude())
                .build();
    }


    // =========================================================

    // 여행 사진 삭제
    @Transactional
    public void deletePhotos(Long tripId, List<Long> photoIds) {
        // 1. tripId에 속한 photoId인지 확인 후 가져옴
        List<Photo> photosToDelete = photoRepository.findAllByIdInAndAlbum_Trip_Id(photoIds, tripId);

        if (photosToDelete.isEmpty()) {
            // 삭제할 사진이 없거나, 유효하지 않은 요청일 경우 종료
            return;
        }

        // 2. S3에서 삭제할 파일 키(key) 목록을 추출
        List<String> s3KeysToDelete = photosToDelete.stream()
                .map(photo -> {
                    try {
                        String fullUrl = photo.getS3Url();
                        String pathWithSlash = new java.net.URL(fullUrl).getPath();
                        String keyWithEncoding = pathWithSlash.substring(1);

                        //UTF-8 형식으로 명시적으로 디코딩
                        return URLDecoder.decode(keyWithEncoding, StandardCharsets.UTF_8);

                    } catch (Exception e) {
                        log.error("S3 URL 처리 중 오류 발생: {}", photo.getS3Url(), e);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        // 3. S3에 있는 파일들을 삭제 (개별 삭제)
        s3KeysToDelete.forEach(s3Uploader::deleteByKey);

        // 4. DB에서 해당 사진들과 관련된 PhotoTag들을 먼저 삭제
        photoTagRepository.deleteAllByPhotoIn(photosToDelete);

        // 5. DB에서 Photo 엔티티들을 삭제
        photoRepository.deleteAll(photosToDelete);
    }


    // 여행 사진 태그 수정(기존 태그 삭제 후 다시 생성)
    @Transactional
    public UploadPhotoResponse updatePhotoTags(Long tripId, Long photoId, UpdatePhotoTagRequest request) {
        // 1. 사진 조회 (photoId와 tripId가 모두 일치하는지 확인)
        Photo photo = photoRepository.findByIdAndAlbum_Trip_Id(photoId, tripId)
                .orElseThrow(() -> new EntityNotFoundException("해당 여행에 존재하지 않는 사진입니다."));

        // 2. 기존 태그 일괄 삭제
        photoTagRepository.deleteAllByPhotoId(photoId);

        List<Long> memberIds = request.getMemberIds();

        // 3. 요청된 memberId가 없다면 빈 태그 목록으로 응답
        if (memberIds == null || memberIds.isEmpty()) {
            return createUploadResponse(photo, Collections.emptyList());
        }

        // 4. 새로운 TripMember 조회 (memberIds와 tripId로 소속 검증)
        List<TripMember> tripMembers = tripMemberRepository.findAllByIdInAndTrip_Id(memberIds, tripId);

        // 요청된 memberId 개수와 실제 조회된 tripMember 개수가 다르면 잘못된 요청
        if (memberIds.size() != tripMembers.size()) {
            throw new IllegalArgumentException("요청된 멤버 ID 중 일부가 해당 여행에 속하지 않습니다.");
        }

        // 5. 새로운 PhotoTag 생성 및 저장
        List<PhotoTag> newPhotoTags = tripMembers.stream()
                .map(tripMember -> new PhotoTag(photo, tripMember.getUser()))
                .collect(Collectors.toList());
        photoTagRepository.saveAll(newPhotoTags);

        // 6. 최종 응답 DTO 생성
        List<UploadPhotoResponse.TaggedUser> taggedUsers = tripMembers.stream()
                .map(tripMember -> UploadPhotoResponse.TaggedUser.builder()
                        .userId(tripMember.getUser().getId())
                        .userName(tripMember.getUser().getName())
                        .build())
                .collect(Collectors.toList());

        return createUploadResponse(photo, taggedUsers);
    }


    // SNAP 페이지 준비 상태 조회 기능
    public SnapReadinessResponse getSnapReadiness(Long tripId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        String cacheKey = "trip::" + tripId + "::snapReadiness";

        try {
            // 1. Redis에서 캐시된 "공용 정보"를 먼저 조회
            String cachedDataJson = redisTemplate.opsForValue().get(cacheKey);
            CachedSnapStatus cachedStatus;

            if (cachedDataJson != null) {
                // [Cache Hit] 캐시가 있다면, "공용 정보" DTO로 변환
                log.info("Cache HIT for key: {}", cacheKey);
                cachedStatus = objectMapper.readValue(cachedDataJson, CachedSnapStatus.class);
                boolean isMember = cachedStatus.getMembers().stream()
                        .anyMatch(m->m.getUserId().equals(currentUserId));
                if(!isMember) {throw new SecurityException("해당 여행의 멤버가 아닙니다.");
                }} else {
                // [Cache Miss] 캐시가 없다면, DB에서 정보를 조회하고 새로운 "공용 정보" 캐시를 생성
                log.info("Cache MISS for key: {}", cacheKey);

                List<TripMember> membersFromDb = tripMemberRepository.findByTripIdWithUser(tripId);
                if (membersFromDb.isEmpty()) {
                    throw new EntityNotFoundException("해당 여행을 찾을 수 없거나 멤버가 없습니다.");
                }
                // 인가 확인: 요청한 사용자가 이 여행의 멤버인지 확인
                membersFromDb.stream()
                        .filter(tm -> tm.getUser().getId().equals(currentUserId))
                        .findFirst()
                        .orElseThrow(() -> new SecurityException("해당 여행의 멤버가 아닙니다."));

                // DB 결과를 "공용 정보" DTO 리스트로 변환합니다.
                List<CachedMemberStatus> memberStatusList = membersFromDb.stream()
                        .map(tm -> CachedMemberStatus.builder()
                                .userId(tm.getUser().getId())
                                .name(tm.getUser().getName())
                                .profileImageUrl(tm.getUser().getProfileImage())
                                .hasFaceData(tm.getUser().getFaceImageUrl() != null)
                                .build())
                        .collect(Collectors.toList());

                boolean allRegistered = memberStatusList.stream().allMatch(CachedMemberStatus::isHasFaceData);

                cachedStatus = CachedSnapStatus.builder()
                        .allMembersRegistered(allRegistered)
                        .members(memberStatusList)
                        .build();

                // 생성된 "공용 정보"를 JSON으로 변환하여 Redis에 저장
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(cachedStatus));
            }

            // 2. 최종적으로, "공용 정보"에 "개인 정보(isCurrentUser)"를 동적으로 추가하여 프론트엔드에 응답
            List<SnapMemberStatusDto> finalMemberList = cachedStatus.getMembers().stream()
                    .map(cm -> SnapMemberStatusDto.builder()
                            .userId(cm.getUserId())
                            .name(cm.getName())
                            .profileImageUrl(cm.getProfileImageUrl())
                            .hasFaceData(cm.isHasFaceData())
                            .isCurrentUser(cm.getUserId().equals(currentUserId)) // ★★★ 이 부분에서 동적으로 계산!
                            .build())
                    .collect(Collectors.toList());

            return SnapReadinessResponse.builder()
                    .allMembersRegistered(cachedStatus.isAllMembersRegistered())
                    .members(finalMemberList)
                    .build();

        } catch (IOException e) {
            // JSON 처리 중 에러 발생 시
            log.warn("Redis 캐시 역직렬화 실패. 캐시 삭제 후 DB 폴백 시도. key={}", cacheKey, e);
            redisTemplate.delete(cacheKey);
            // 폴백: DB에서 재생성
            return getSnapReadiness(tripId);
        }
    }


    // 특정 여행의 사진 목록을 페이지네이션으로 조회
    @Transactional(readOnly = true)
    public PhotoPageResponse getSnapPhotos(Long tripId, int page, String sortKey) {
        Album album = albumRepository.findByTripId(tripId)
                .orElseThrow(() -> new EntityNotFoundException("해당 여행의 앨범을 찾을 수 없습니다."));

        validatePageAndSort(page, sortKey);

        Pageable pageable = buildPageable(page, sortKey); // size=15 고정
        Slice<Photo> slice = photoRepository.findByAlbum_Id(album.getId(), pageable);

        return buildPhotoPageResponse(slice, page);
    }

    // 특정 여행에서 특정 멤버가 태그된 사진 목록을 페이지네이션으로 조회
    @Transactional(readOnly = true)
    public PhotoPageResponse getSnapPhotosByMember(Long tripId, Long memberId, int page, String sortKey) {

        // 요청한 memberId가 실제로 해당 tripId에 속한 멤버인지 먼저 확인합니다.
        boolean isMember = tripMemberRepository.existsByTrip_IdAndUser_Id(tripId, memberId);
        if (!isMember) {
            // 멤버가 아니라면, 사진을 찾을 필요도 없이 바로 404 에러를 발생시킵니다.
            throw new EntityNotFoundException("해당 여행에 존재하지 않는 멤버입니다. (ID: " + memberId + ")");
        }

        Album album = albumRepository.findByTripId(tripId)
                .orElseThrow(() -> new EntityNotFoundException("해당 여행의 앨범을 찾을 수 없습니다."));

        validatePageAndSort(page, sortKey);

        Pageable pageable = buildPageable(page, sortKey);
        Slice<Photo> slice = photoRepository.findTaggedPhotosByAlbumAndUser(album.getId(), memberId, pageable);

        return buildPhotoPageResponse(slice, page);
    }


    private void verifySnapReadiness(Long tripId) {
        if (readinessCheckEnabled) {
            List<TripMember> members = tripMemberRepository.findByTripIdWithUser(tripId);
            boolean allRegistered = members.stream()
                    .allMatch(member -> member.getUser().getFaceImageUrl() != null);
            if (!allRegistered) {
                throw new IllegalStateException("모든 여행 멤버의 얼굴이 등록되지 않아 Snap 기능을 사용할 수 없습니다.");
            }
        }
    }



    private void validatePageAndSort(int page, String sortKey) {
        if (page < 0) throw new IllegalArgumentException("page must be >= 0");
        if (!"date_desc".equalsIgnoreCase(sortKey) && !"date_asc".equalsIgnoreCase(sortKey)) {
            throw new IllegalArgumentException("sort must be one of [date_desc, date_asc]");
        }
    }

    private Pageable buildPageable(int page, String sortKey) {
        boolean asc = "date_asc".equalsIgnoreCase(sortKey);

        Sort sort = asc
                ? Sort.by(
                Sort.Order.asc("photoDt").nullsLast(),
                Sort.Order.asc("id")
        )
                : Sort.by(
                Sort.Order.desc("photoDt").nullsLast(),
                Sort.Order.desc("id")
        );

        return PageRequest.of(page, PAGE_SIZE, sort);
    }

    private PhotoPageResponse buildPhotoPageResponse(Slice<Photo> slice, int currentPage) {
        List<PhotoPageResponse.PhotoDetailDto> items = slice.getContent().stream()
                .map(this::toPhotoDetailDto)
                .toList();

        return PhotoPageResponse.builder()
                .photos(items)
                .currentPage(currentPage)
                .isLast(slice.isLast())
                .build();
    }

    private PhotoPageResponse.PhotoDetailDto toPhotoDetailDto(Photo p) {
        return PhotoPageResponse.PhotoDetailDto.builder()
                .photoId(p.getId())
                .photoUrl(p.getS3Url())
                .photoDate(p.getPhotoDt() != null
                        ? p.getPhotoDt().toLocalDate()
                        : null)
                .taggedUsers(
                        p.getPhotoTags().stream()
                                .map(PhotoTag::getUser)
                                .map(u -> PhotoPageResponse.TaggedUserDto.builder()
                                        .userId(u.getId())
                                        .name(u.getName())
                                        .build())
                                .toList()
                )
                .build();
    }

}