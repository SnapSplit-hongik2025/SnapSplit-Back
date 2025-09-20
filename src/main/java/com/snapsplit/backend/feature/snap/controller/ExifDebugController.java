package com.snapsplit.backend.feature.snap.controller;

import com.snapsplit.backend.feature.snap.service.ExifService;
import com.snapsplit.backend.feature.snap.service.ExifService.ExifData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/debug/exif")
public class ExifDebugController {

    private final ExifService exifService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> read(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "file is required"
            ));
        }

        return exifService.extract(file)
                .<ResponseEntity<?>>map((ExifData exif) -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "hasExif", true,
                        "latitude", exif.getLatitude(),
                        "longitude", exif.getLongitude(),
                        "takenAtLocal", exif.getTakenAtLocal() // 타임존 없는 현지 시각
                )))
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "hasExif", false,
                        "message", "No EXIF GPS/DateTime found"
                )));
    }
}
