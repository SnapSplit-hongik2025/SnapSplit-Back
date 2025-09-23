package com.snapsplit.backend.feature.snap.service;

import com.snapsplit.backend.config.properties.AwsProperties;
import com.snapsplit.backend.domain.album.entity.Album;
import com.snapsplit.backend.domain.album.repository.AlbumRepository;
import com.snapsplit.backend.domain.photo.entity.Photo;
import com.snapsplit.backend.domain.photo.repository.PhotoRepository;
import com.snapsplit.backend.feature.snap.dto.DownloadRequest;
import com.snapsplit.backend.global.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
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
public class DownloadService {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;
    private final PhotoRepository photoRepository;
    private final AlbumRepository albumRepository;

    public void streamZip(Long tripId, DownloadRequest request, HttpServletResponse response) throws Exception {
        String zipName = "photos.zip";
        String encoded = URLEncoder.encode(zipName, StandardCharsets.UTF_8);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + zipName + "\"; filename*=UTF-8''" + encoded);

        Album album = albumRepository.findByTripId(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 tripId에 album이 존재하지 않습니다."));


        List<Photo> photos = photoRepository.findAllById(request.getPhotoIds());

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (Photo photo : photos) {
                // tripId 검증 (안 하면 다른 트립 사진도 다운로드 될 수 있음)
                if (!photo.getAlbum().getId().equals(album.getId())) {
                    throw new ForbiddenException("권한 없는 사진 요청으로 응답이 거부되었습니다.");
                }

                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(awsProperties.getS3().getBucket())
                        .key(photo.getObjectKey())
                        .build();

                try (ResponseInputStream<?> s3is = s3Client.getObject(getObjectRequest)) {
                    zos.putNextEntry(new ZipEntry(photo.getFileName())); // zip 안 이름
                    s3is.transferTo(zos);
                    zos.closeEntry();
                }
            }
            zos.finish();
        }
    }
}
