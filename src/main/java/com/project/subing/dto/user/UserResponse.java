package com.project.subing.dto.user;

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
public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private String token;
    private UserRole role;
    private UserTier tier;
    private LocalDateTime createdAt;
    @Builder.Default
    private boolean isNewUser = false;
}
