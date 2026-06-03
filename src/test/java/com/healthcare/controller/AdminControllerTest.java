package com.healthcare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.dto.request.AdminRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.entity.User;
import com.healthcare.security.JwtAuthenticationFilter;
import com.healthcare.security.JwtService;
import com.healthcare.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@DisplayName("AdminController Web Layer Tests")
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AdminService adminService;
    @MockBean JwtService jwtService;

    private ApiResponse.UserResponse mockUserResponse(User.Role role) {
        return new ApiResponse.UserResponse(
                UUID.randomUUID(), "Test User", "test@example.com",
                role, "en", true, LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/users — 200 for ADMIN role")
    void getAllUsers_adminAccess() throws Exception {
        var paged = new ApiResponse.PagedResponse<>(
                List.of(mockUserResponse(User.Role.USER)), 0, 20, 1L, 1, true);
        when(adminService.getAllUsers(any())).thenReturn(paged);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/admin/users — 403 for non-ADMIN role")
    void getAllUsers_nonAdminDenied() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/admin/users/{id}/role — 200 on role update")
    void updateUserRole_success() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminService.updateUserRole(eq(userId), any()))
                .thenReturn(mockUserResponse(User.Role.DOCTOR));

        mockMvc.perform(patch("/api/admin/users/" + userId + "/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminRequest.UpdateUserRoleRequest(User.Role.DOCTOR))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("DOCTOR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/admin/users/{id}/status — 200 on status update")
    void updateUserStatus_success() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminService.updateUserStatus(eq(userId), any()))
                .thenReturn(mockUserResponse(User.Role.USER));

        mockMvc.perform(patch("/api/admin/users/" + userId + "/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminRequest.UpdateUserStatusRequest(false))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/users/{id} — 200 on deletion")
    void deleteUser_success() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(adminService).deleteUser(userId);

        mockMvc.perform(delete("/api/admin/users/" + userId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
