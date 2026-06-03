package com.healthcare.dto.request;

import jakarta.validation.constraints.*;

public class AuthRequest {

    public record RegisterRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
            @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Password must contain at least one uppercase, one lowercase, and one digit"
            )
            String password,

            @Size(min = 2, max = 10, message = "Language code must be between 2 and 10 characters")
            String language
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    public record UpdateProfileRequest(
            @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
            String name,

            @Size(min = 2, max = 10, message = "Language code must be between 2 and 10 characters")
            String language
    ) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required")
            String currentPassword,

            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
            @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Password must contain uppercase, lowercase, and digit"
            )
            String newPassword
    ) {}
}
