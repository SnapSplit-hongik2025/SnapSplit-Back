package com.snapsplit.backend.feature.face.controller;

import com.snapsplit.backend.feature.face.service.FaceService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
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
    @Operation(summary = "사용자 얼굴 정보 최초 등록", description = "사용자의 최초 얼굴 정보 등록하기")
    public ResponseEntity<ApiResponse<Void>> registerMyFace(
            @RequestParam("image") MultipartFile faceImage
    ) {
        try{
            faceService.registerFace(faceImage);
            return ResponseEntity.ok(ApiResponse.success("얼굴이 성공적으로 등록되었습니다.", null));
        } catch (IllegalStateException e) {
            log.warn("얼굴 등록 실패 (이미 등록됨): {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.fail(HttpStatus.CONFLICT.value(), e.getMessage()), HttpStatus.CONFLICT);
        } catch (IllegalArgumentException e) {
            log.warn("얼굴 등록 실패 (잘못된 요청): {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            log.error("파일 처리 중 오류 발생", e);
            return new ResponseEntity<>(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "이미지 처리 중 오류가 발생했습니다."), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 사용자 얼굴 정보 수정 API
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "사용자 얼굴 정보 수정", description = "기존에 등록된 얼굴 정보를 새로운 사진으로 교체합니다.")
    public ResponseEntity<ApiResponse<Void>> updateMyFace(
            @RequestParam("image") MultipartFile faceImage
    ) {
        try {
            faceService.updateFace(faceImage);
            return ResponseEntity.ok(ApiResponse.success("얼굴 정보가 성공적으로 수정되었습니다.", null));
        } catch (EntityNotFoundException e) {
            log.warn("얼굴 수정 실패 (등록된 정보 없음): {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.fail(HttpStatus.NOT_FOUND.value(), e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException e) {
            log.warn("얼굴 수정 실패 (잘못된 요청): {}", e.getMessage());
            return new ResponseEntity<>(ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            log.error("파일 처리 중 오류 발생", e);
            return new ResponseEntity<>(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "이미지 처리 중 오류가 발생했습니다."), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}