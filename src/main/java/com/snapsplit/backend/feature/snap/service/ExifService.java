package com.snapsplit.backend.feature.snap.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Date;
import java.util.Optional;

@Service
public class ExifService {

    /**
     * 업로드된 파일에서 EXIF 정보 추출
     * @param file 업로드된 사진 파일
     * @return Optional<ExifData> (위치, 촬영시간 포함)
     */
    public Optional<ExifData> extract(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(in);

            // GPS 좌표
            Double lat = null, lon = null;
            GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gps != null && gps.getGeoLocation() != null) {
                lat = gps.getGeoLocation().getLatitude();
                lon = gps.getGeoLocation().getLongitude();
            }

            // 촬영 시간
            Date takenAt = null;
            ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exif != null) {
                takenAt = exif.getDateOriginal(); // DateTimeOriginal
                if (takenAt == null) {
                    takenAt = exif.getDateDigitized();
                }
            }

            if (lat == null && lon == null && takenAt == null) {
                return Optional.empty();
            }

            return Optional.of(new ExifData(lat, lon, takenAt));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Value
    public static class ExifData {
        Double latitude;      // 위도
        Double longitude;     // 경도
        Date takenAtLocal;    // 촬영 시각 (타임존 정보 없음)
    }
}
