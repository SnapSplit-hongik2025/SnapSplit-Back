package com.snapsplit.backend.feature.snap.controller;

import com.snapsplit.backend.feature.snap.dto.*;
import com.snapsplit.backend.feature.snap.service.DownloadService;
import com.snapsplit.backend.feature.snap.service.SnapService;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Tag(name = "SNAP", description = "얼굴 등록, 사진 업로드, 필터링, 사진 ZIP 다운로드")
@RestController
@RequestMapping("/trips/{tripId}/snap")
@RequiredArgsConstructor
@Slf4j
public class SnapController {

    private final SnapService snapService;
    private final DownloadService downloadService;

    @CheckTripMember
    @GetMapping("/readiness")
    @Operation(summary = "SNAP 페이지 멤버 상태 조회", description = "모든 여행 멤버의 얼굴 등록 여부를 확인합니다.")
    public ResponseEntity<ApiResponse<SnapReadinessResponse>> getSnapReadiness(
            @PathVariable Long tripId
    ) {
        SnapReadinessResponse response = snapService.getSnapReadiness(tripId);
        return ResponseEntity.ok(ApiResponse.success("SNAP 준비 상태 조회 성공", response));
    }

    @CheckTripMember
    @GetMapping("/photos")
    @Operation(summary = "Snap 사진 목록 조회", description = "여행에 업로드된 사진들을 페이지네이션으로 조회합니다.")
    public ResponseEntity<ApiResponse<PhotoPageResponse>> getSnapPhotos(
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "date_desc") String sort // date_desc | date_asc
    ) {
        PhotoPageResponse response = snapService.getSnapPhotos(tripId, page, sort);
        return ResponseEntity.ok(ApiResponse.success("사진 목록 조회 성공", response));
    }

    @CheckTripMember
    @GetMapping("/folders/{memberId}")
    @Operation(summary = "인물별 폴더 상세 조회", description = "특정 멤버가 태그된 사진들을 페이지네이션으로 조회합니다.")
    public ResponseEntity<ApiResponse<PhotoPageResponse>> getSnapPhotosByMember(
            @PathVariable Long tripId,
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "date_desc") String sort // date_desc | date_asc
    ) {
        PhotoPageResponse response = snapService.getSnapPhotosByMember(tripId, memberId, page, sort);
        return ResponseEntity.ok(ApiResponse.success("인물별 사진 목록 조회 성공", response));
    }

    @CheckTripMember
    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "사진 업로드 및 자동 태깅", description = "여행에 사진을 업로드하고 자동으로 인물을 태깅합니다.")
    public ResponseEntity<ApiResponse<List<UploadPhotoResponse>>> uploadPhotos(
            @PathVariable Long tripId,
            @RequestParam("images") List<MultipartFile> images
    ) {
        log.info("[CONTROLLER] 요청 도착 tripId={}, 파일개수={}", tripId, images.size());
        images.forEach(f ->
                log.info("[CONTROLLER] 파일명={}, size={} bytes, contentType={}",
                        f.getOriginalFilename(), f.getSize(), f.getContentType())
        );
        List<UploadPhotoResponse> responseData = snapService.uploadAndTagPhotos(tripId, images);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("사진 업로드 및 태깅에 성공했습니다.", responseData));
    }

    @CheckTripMember
    @DeleteMapping("/photos")
    @Operation(summary = "사진 삭제", description = "여행에 업로드된 사진을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deletePhotos(
            @PathVariable Long tripId,
            @RequestBody DeletePhotoRequest request
    ) {
        snapService.deletePhotos(tripId, request.getPhotoIds());
        return ResponseEntity.ok(ApiResponse.success("요청된 사진들이 삭제되었습니다.", null));
    }

    @CheckTripMember
    @PutMapping("/photos/{photoId}/tags")
    @Operation(summary = "사진 태그 수동 수정", description = "사진의 인물 태그를 요청된 멤버 목록으로 교체합니다.")
    public ResponseEntity<ApiResponse<UploadPhotoResponse>> updatePhotoTags(
            @PathVariable Long tripId,
            @PathVariable Long photoId,
            @RequestBody UpdatePhotoTagRequest request
    ) {
        UploadPhotoResponse responseData = snapService.updatePhotoTags(tripId, photoId, request);
        return ResponseEntity.ok(ApiResponse.success("사진의 인물 태그가 성공적으로 수정되었습니다.", responseData));
    }

    @CheckTripMember
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