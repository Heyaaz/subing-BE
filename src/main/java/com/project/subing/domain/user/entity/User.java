package com.project.subing.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(nullable = false, length = 255)
    private String password;
    
    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserTier tier = UserTier.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // 양방향 연관관계는 나중에 추가 (순환 참조 방지)
    
    // 비즈니스 로직
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateName(String newName) {
        this.name = newName;
    }

    public void upgradeTier(UserTier newTier) {
        this.tier = newTier;
    }

    public void updateRole(UserRole newRole) {
        this.role = newRole;
    }

    public boolean isPro() {
        return this.tier == UserTier.PRO;
    }

    public boolean isFree() {
        return this.tier == UserTier.FREE;
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }
}
