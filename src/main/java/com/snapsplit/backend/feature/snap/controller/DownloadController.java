package com.snapsplit.backend.feature.snap.controller;

import com.snapsplit.backend.feature.snap.dto.DownloadRequest;
import com.snapsplit.backend.feature.snap.service.DownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "SNAP", description = "사진 ZIP 다운로드")
@RestController
@RequestMapping("/trips/{tripId}/snap")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;

    @Operation(summary = "선택한 사진들을 ZIP 파일로 다운로드")
    @PostMapping("/download")
    public void downloadPhotos(
            @PathVariable Long tripId,
            @RequestBody DownloadRequest request,
            HttpServletResponse response
    ) throws Exception {
        downloadService.streamZip(tripId, request, response);
    }
}
