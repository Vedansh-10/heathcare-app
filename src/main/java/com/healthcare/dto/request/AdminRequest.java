package com.healthcare.dto.request;

import com.healthcare.entity.User;
import jakarta.validation.constraints.*;

public class AdminRequest {

    public record UpdateUserRoleRequest(
            @NotNull(message = "Role is required")
            User.Role role
    ) {}

    public record UpdateUserStatusRequest(
            @NotNull(message = "Active status is required")
            Boolean isActive
    ) {}
}
