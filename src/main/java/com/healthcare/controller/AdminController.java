package com.healthcare.controller;

import com.healthcare.dto.request.AdminRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.entity.User;
import com.healthcare.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only user management endpoints")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Get all users (paginated)")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.PagedResponse<ApiResponse.UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Users retrieved", adminService.getAllUsers(pageable)));
    }

    @GetMapping("/users/role/{role}")
    @Operation(summary = "Get users by role")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.PagedResponse<ApiResponse.UserResponse>>> getUsersByRole(
            @PathVariable User.Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Users retrieved",
                adminService.getUsersByRole(role, pageable)));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.UserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of(adminService.getUserById(id)));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.UserResponse>> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody AdminRequest.UpdateUserRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Role updated",
                adminService.updateUserRole(id, request)));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Activate or deactivate a user")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.UserResponse>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdminRequest.UpdateUserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Status updated",
                adminService.updateUserStatus(id, request)));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Permanently delete a user")
    public ResponseEntity<ApiResponse.SuccessResponse<Void>> deleteUser(@PathVariable UUID id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("User deleted", null));
    }
}
