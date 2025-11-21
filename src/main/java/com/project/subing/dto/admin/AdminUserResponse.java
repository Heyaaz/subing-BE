package com.project.subing.dto.admin;

import com.project.subing.domain.user.entity.User;
import com.project.subing.domain.user.entity.UserRole;
import com.project.subing.domain.user.entity.UserTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String email;
    private String name;
    private UserTier tier;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .tier(user.getTier())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}