package com.project.subing.dto.admin;

import com.project.subing.domain.user.entity.UserRole;
import com.project.subing.domain.user.entity.UserTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private UserTier tier;
    private UserRole role;
}