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

            // 촬영 시간
            Date takenAt = null;
            ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exif != null) {
                takenAt = exif.getDateOriginal(); // DateTimeOriginal
                if (takenAt == null) {
                    takenAt = exif.getDateDigitized();
                }
            }

            return Optional.of(new ExifData(takenAt));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Value
    public static class ExifData {
        Date takenAtLocal;    // 촬영 시각 (타임존 정보 없음)
    }
}
