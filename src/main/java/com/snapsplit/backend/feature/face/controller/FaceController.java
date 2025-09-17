package com.snapsplit.backend.feature.face.controller;

import com.snapsplit.backend.feature.face.service.FaceService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "마이페이지", description = "마이페이지 조회/수정")
@Slf4j
@RestController
@RequestMapping("/api/snap/face")
@RequiredArgsConstructor
public class FaceController {

    private final FaceService faceService;

    // 사용자 얼굴 정보 등록 API
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "사용자 얼굴 정보 등록", description = "사용자의 최초 얼굴 정보 등록하기, faceId를 user 정보에 추가합니다. ")
    public ResponseEntity<ApiResponse<Void>> registerMyFace(
            @RequestParam("image") MultipartFile faceImage
    ) {
        try{
            faceService.registerFace(faceImage);
            return ResponseEntity.ok(ApiResponse.success("얼굴이 성공적으로 등록되었습니다.", null));
        } catch (IllegalStateException e){
            log.warn("얼굴 등록 실패 (이미 등록됨): {}", e.getMessage());
            final int statusCode = HttpStatus.CONFLICT.value();
            final ApiResponse<Void> response = ApiResponse.fail(statusCode, e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        } catch (IOException e) {
            log.error("파일 처리 중 오류 발생", e);
            final int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
            final ApiResponse<Void> response = ApiResponse.fail(statusCode, "이미지 처리 중 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    // 사용자 얼굴 정보 삭제 API
    @DeleteMapping
    @Operation(summary = "사용자 얼굴 정보 삭제", description = "사용자의 얼굴 정보 삭제하기, 이미 분류된 사진들이 있다면 주의가 필요합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteMyFace() {
        try {
            faceService.deleteFace();
            return ResponseEntity.ok(ApiResponse.success("등록된 얼굴 정보가 삭제되었습니다.", null));
        } catch (IllegalStateException e) {
            log.warn("얼굴 삭제 실패 (등록되지 않음): {}", e.getMessage());
            final int statusCode = HttpStatus.BAD_REQUEST.value();
            final ApiResponse<Void> response = ApiResponse.fail(statusCode, e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}