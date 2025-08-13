package com.snapsplit.backend.feature.face.controller;

import com.snapsplit.backend.feature.face.service.FaceService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "SNAP", description = "얼굴 등록, 사진 업로드, 필터링")
@RestController
@RequestMapping("/api/snap/face")
@RequiredArgsConstructor
public class FaceController {

    private final FaceService faceService;

    // 사용자 얼굴 정보 등록 API
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "사용자 얼굴 정보 등록", description = "사용자의 최초 얼굴 정보 등록하기, faceId를 user 정보에 추가합니다. ")
    public ResponseEntity<ApiResponse<Void>> registerMyFace(
            // Todo: Spring Security 적용 후 @AuthenticationPrincipal로 사용자 정보 받아오기
            @RequestParam("userId") Long userId,
            @RequestParam("image") MultipartFile faceImage
    ) throws IOException {
        faceService.registerFace(userId, faceImage);
        return ResponseEntity.ok(ApiResponse.success("얼굴이 성공적으로 등록되었습니다.", null));
    }

    // 사용자 얼굴 정보 삭제 API
    @DeleteMapping
    @Operation(summary = "사용자 얼굴 정보 삭제", description = "사용자의 얼굴 정보 삭제하기, 이미 분류된 사진들이 있다면 주의가 필요합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteMyFace(
            // Todo: Spring Security 적용 후 @AuthenticationPrincipal로 사용자 정보 받아오기
            @RequestParam("userId") Long userId
    ) {
        faceService.deleteFace(userId);
        return ResponseEntity.ok(ApiResponse.success("등록된 얼굴 정보가 삭제되었습니다.", null));
    }
}