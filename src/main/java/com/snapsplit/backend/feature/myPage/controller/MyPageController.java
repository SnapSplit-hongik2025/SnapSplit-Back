package com.snapsplit.backend.feature.myPage.controller;

import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.feature.myPage.dto.MyFaceResponse;
import com.snapsplit.backend.feature.myPage.dto.MyPageResponse;
import com.snapsplit.backend.feature.myPage.service.MyPageService;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.global.response.ApiResponse;
import com.snapsplit.backend.global.s3.S3Uploader;
import com.snapsplit.backend.global.s3.dto.S3UploadResult;
import com.snapsplit.backend.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Tag(name = "마이페이지", description = "마이페이지 조회/수정")
@RestController
@RequiredArgsConstructor
@RequestMapping("/home/myPage")
public class MyPageController {

    private final MyPageService myPageService;
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;

    // GET /home/myPage
    // 마이페이지 조회
    @Operation(summary = "마이페이지 조회", description = "마이페이지 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long userId = principal.getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        MyPageResponse response = myPageService.getMyPage(user);
        return ResponseEntity.ok(ApiResponse.success("마이페이지 조회 성공", response));
    }

    // PUT /home/myPage
    // 마이페이지 수정
    @Operation(summary = "마이페이지 수정", description = "기존 마이페이지 정보를 수정합니다.")
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) MultipartFile profileImage
    ) throws IOException {
        Long userId = principal.getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        String profileImageUrl = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            S3UploadResult uploadResult = s3Uploader.upload(profileImage, "profile");
            profileImageUrl = uploadResult.getFileUrl();
        }

        myPageService.updateProfile(user, name, profileImageUrl);
        return ResponseEntity.ok(ApiResponse.success("프로필 수정 완료", null));
    }

    // GET /home/myPage/face
    // 나의 얼굴 관리하기 페이지
    @Operation(summary = "나의 얼굴 관리 페이지 조회", description = "얼굴 등록 여부와 등록된 얼굴 이미지 URL을 조회합니다.")
    @GetMapping("/face")
    public ResponseEntity<ApiResponse<MyFaceResponse>> getMyFacePage(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long userId = principal.getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        MyFaceResponse response = myPageService.getMyFaceInfo(user);

        return ResponseEntity.ok(ApiResponse.success("나의 얼굴 정보 조회 성공", response));
    }
}
