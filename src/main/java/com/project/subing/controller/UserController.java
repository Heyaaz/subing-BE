package com.project.subing.controller;

import com.project.subing.domain.user.entity.UserTier;
import com.project.subing.dto.common.ApiResponse;
import com.project.subing.dto.user.LoginRequest;
import com.project.subing.dto.user.SignupRequest;
import com.project.subing.dto.user.UserResponse;
import com.project.subing.dto.user.UserTierInfoResponse;
import com.project.subing.service.UserService;
import com.project.subing.service.TierLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TierLimitService tierLimitService;
    
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest request) {
        UserResponse response = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입이 완료되었습니다."));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request) {
        UserResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "로그인에 성공했습니다."));
    }

    @GetMapping("/{userId}/tier-info")
    public ResponseEntity<ApiResponse<UserTierInfoResponse>> getTierInfo(@PathVariable Long userId) {
        UserTierInfoResponse response = userService.getUserTierInfo(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "티어 정보를 조회했습니다."));
    }

    @PutMapping("/{userId}/upgrade-tier")
    public ResponseEntity<ApiResponse<Void>> upgradeTier(
            @PathVariable Long userId,
            @RequestParam UserTier newTier) {
        tierLimitService.upgradeTier(userId, newTier);
        return ResponseEntity.ok(ApiResponse.success(null, "티어가 업그레이드되었습니다."));
    }
}
