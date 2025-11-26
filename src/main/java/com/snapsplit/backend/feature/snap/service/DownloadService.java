package com.snapsplit.backend.feature.snap.service;

import com.snapsplit.backend.config.properties.AwsProperties;
import com.snapsplit.backend.domain.album.entity.Album;
import com.snapsplit.backend.domain.album.repository.AlbumRepository;
import com.snapsplit.backend.domain.photo.entity.Photo;
import com.snapsplit.backend.domain.photo.repository.PhotoRepository;
import com.snapsplit.backend.feature.snap.dto.DownloadRequest;
import com.snapsplit.backend.global.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DownloadService {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;
    private final PhotoRepository photoRepository;
    private final AlbumRepository albumRepository;

    public void streamZip(Long tripId, DownloadRequest request, HttpServletResponse response) throws Exception {

        log.info("📥 [DOWNLOAD] 요청 들어옴 tripId={}, photoIds={}", tripId, request.getPhotoIds());

        String zipName = "photos.zip";
        String encoded = URLEncoder.encode(zipName, StandardCharsets.UTF_8);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + zipName + "\"; filename*=UTF-8''" + encoded);

        log.info("📦 [DOWNLOAD] ZIP 파일 헤더 설정 완료. zipName={}", zipName);

        // 앨범 조회
        Album album = albumRepository.findByTripId(tripId)
                .orElseThrow(() -> {
                    log.error("❌ [DOWNLOAD] album 조회 실패. tripId={}", tripId);
                    return new IllegalArgumentException("해당 tripId에 album이 존재하지 않습니다.");
                });

        log.info("📂 [DOWNLOAD] 앨범 조회 성공 albumId={}", album.getId());


        // 사진 조회
        List<Photo> photos = photoRepository.findAllById(request.getPhotoIds());
        log.info("📸 [DOWNLOAD] 사진 개수 조회 완료 count={}", photos.size());

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {

            for (Photo photo : photos) {

                log.info("➡️ [DOWNLOAD] ZIP 엔트리 시작 fileName={} objectKey={}",
                        photo.getFileName(), photo.getObjectKey());

                // 트립 권한 검증
                if (!photo.getAlbum().getId().equals(album.getId())) {
                    log.warn("⛔ [DOWNLOAD] 권한 없는 사진 요청 photoId={}, albumId={}, 요청 albumId={}",
                            photo.getId(), photo.getAlbum().getId(), album.getId());
                    throw new ForbiddenException("권한 없는 사진 요청으로 응답이 거부되었습니다.");
                }

                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(awsProperties.getS3().getBucket())
                        .key(photo.getObjectKey())
                        .build();

                log.info("🧲 [DOWNLOAD] S3 요청 시작 bucket={}, key={}",
                        awsProperties.getS3().getBucket(), photo.getObjectKey());

                try (ResponseInputStream<?> s3is = s3Client.getObject(getObjectRequest)) {

                    log.info("✔️ [DOWNLOAD] S3 다운로드 성공 fileName={}", photo.getFileName());

                    zos.putNextEntry(new ZipEntry(photo.getFileName()));
                    s3is.transferTo(zos);
                    zos.closeEntry();

                    log.info("📤 [DOWNLOAD] ZIP 엔트리 완료 fileName={}", photo.getFileName());
                }
            }

            zos.finish();
            log.info("🎉 [DOWNLOAD] ZIP 생성 완료");
        }

        log.info("🏁 [DOWNLOAD] 전체 ZIP 스트리밍 작업 종료");
    }
}
