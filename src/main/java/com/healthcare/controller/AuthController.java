package com.healthcare.controller;

import com.healthcare.dto.request.AuthRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User registration, login, token management and profile")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.AuthResponse>> register(
            @Valid @RequestBody AuthRequest.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.SuccessResponse.of("User registered successfully", authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT tokens")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.AuthResponse>> login(
            @Valid @RequestBody AuthRequest.LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Login successful", authService.login(request)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of(authService.getCurrentUser(userDetails.getUsername())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile (name, language)")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.UserResponse>> updateProfile(
            @Valid @RequestBody AuthRequest.UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Profile updated",
                authService.updateProfile(userDetails.getUsername(), request)));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<ApiResponse.SuccessResponse<Void>> changePassword(
            @Valid @RequestBody AuthRequest.ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Password changed successfully", null));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<ApiResponse.SuccessResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Logged out successfully", null));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Deactivate own account")
    public ResponseEntity<ApiResponse.SuccessResponse<Void>> deactivateAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.deactivateAccount(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Account deactivated", null));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.AuthResponse>> refreshToken(
            @Valid @RequestBody AuthRequest.RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Token refreshed", authService.refreshToken(request)));
    }
}
