package com.project.subing.controller;

import com.project.subing.dto.admin.AdminUserResponse;
import com.project.subing.dto.admin.UserUpdateRequest;
import com.project.subing.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        List<AdminUserResponse> users = userService.getAllUsersForAdmin();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long userId) {
        AdminUserResponse user = userService.getUserByIdForAdmin(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable Long userId,
            @RequestBody UserUpdateRequest request) {
        AdminUserResponse updatedUser = userService.updateUserByAdmin(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUserByAdmin(userId);
        return ResponseEntity.noContent().build();
    }
}