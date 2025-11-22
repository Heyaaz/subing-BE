package com.project.subing.service;

import com.project.subing.domain.user.entity.User;
import com.project.subing.domain.user.entity.UserRole;
import com.project.subing.domain.user.entity.UserTier;
import com.project.subing.domain.user.entity.UserTierUsage;
import com.project.subing.dto.admin.AdminUserResponse;
import com.project.subing.dto.admin.UserUpdateRequest;
import com.project.subing.dto.user.LoginRequest;
import com.project.subing.dto.user.SignupRequest;
import com.project.subing.dto.user.UserResponse;
import com.project.subing.dto.user.UserTierInfoResponse;
import com.project.subing.exception.business.DuplicateEmailException;
import com.project.subing.exception.business.InvalidCredentialsException;
import com.project.subing.exception.entity.UserNotFoundException;
import com.project.subing.repository.UserRepository;
import com.project.subing.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TierLimitService tierLimitService;
    
    public UserResponse signup(SignupRequest request) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        // 사용자 생성 (BCrypt로 비밀번호 암호화)
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .tier(UserTier.FREE)
                .role(UserRole.USER)
                .build();

        User savedUser = userRepository.save(user);

        // JWT 토큰 생성 (role 포함)
        String token = jwtTokenProvider.createToken(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getRole().name()
        );

        return UserResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .token(token)
                .role(savedUser.getRole())
                .tier(savedUser.getTier())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }
    
    public UserResponse login(LoginRequest request) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException());

        // 비밀번호 검증 (BCrypt 사용)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // JWT 토큰 생성 (role 포함)
        String token = jwtTokenProvider.createToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .token(token)
                .role(user.getRole())
                .tier(user.getTier())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public UserTierInfoResponse getUserTierInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        UserTierUsage usage = tierLimitService.getCurrentMonthUsage(userId);
        int remainingGpt = tierLimitService.getRemainingGptRecommendations(userId);
        int remainingOptimization = tierLimitService.getRemainingOptimizationChecks(userId);

        return UserTierInfoResponse.from(user, usage, remainingGpt, remainingOptimization);
    }

    // ========== 관리자 전용 메서드 ==========

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsersForAdmin() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(AdminUserResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserByIdForAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return AdminUserResponse.from(user);
    }

    public AdminUserResponse updateUserByAdmin(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.getTier() != null) {
            user.upgradeTier(request.getTier());
        }
        if (request.getRole() != null) {
            user.updateRole(request.getRole());
        }

        return AdminUserResponse.from(user);
    }

    public void deleteUserByAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.delete(user);
    }
}
