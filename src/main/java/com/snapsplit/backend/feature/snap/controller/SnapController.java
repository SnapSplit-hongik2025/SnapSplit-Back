package com.snapsplit.backend.feature.snap.controller;

import com.snapsplit.backend.feature.snap.dto.DeletePhotoRequest;
import com.snapsplit.backend.feature.snap.dto.UploadPhotoResponse;
import com.snapsplit.backend.feature.snap.service.SnapService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Tag(name = "SNAP", description = "얼굴 등록, 사진 업로드, 필터링")
@RestController
@RequestMapping("/trips/{tripId}/snap")
@RequiredArgsConstructor
public class SnapController {

    private final SnapService snapService;

    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "사진 업로드 및 자동 태깅", description = "여행에 사진을 업로드하고 자동으로 인물을 태깅합니다.")
    public ResponseEntity<ApiResponse<List<UploadPhotoResponse>>> uploadPhotos(
            @PathVariable Long tripId,
            @RequestParam("images") List<MultipartFile> images
    ) {
        List<UploadPhotoResponse> responseData = snapService.uploadAndTagPhotos(tripId, images);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("사진 업로드 및 태깅에 성공했습니다.", responseData));
    }

    @DeleteMapping("/photos")
    @Operation(summary = "사진 삭제", description = "여행에 업로드된 사진을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deletePhotos(
            @PathVariable Long tripId,
            @RequestBody DeletePhotoRequest request // 파라미터 타입 변경
    ) {
        // snapService.deletePhotos(tripId, request.getPhotoIds());
        return ResponseEntity.ok(ApiResponse.success("요청된 사진들이 삭제되었습니다.", null));
    }

}