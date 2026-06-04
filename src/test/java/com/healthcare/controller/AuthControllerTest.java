package com.healthcare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.config.RateLimitConfig;
import com.healthcare.config.SecurityConfig;
import com.healthcare.dto.request.AuthRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.entity.User;
import com.healthcare.security.JwtAuthenticationFilter;
import com.healthcare.security.JwtService;
import com.healthcare.security.UserDetailsServiceImpl;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import com.healthcare.service.AuthService;
import com.healthcare.util.RateLimitFilter;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
//        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {RateLimitConfig.class, RateLimitFilter.class, SecurityConfig.class, JwtAuthenticationFilter.class})
)

@MockBean(JpaMetamodelMappingContext.class)
@DisplayName("AuthController Web Layer Tests")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    private ApiResponse.AuthResponse mockAuthResponse() {
        var userResp = new ApiResponse.UserResponse(
                UUID.randomUUID(), "Test User", "test@example.com",
                User.Role.USER, "en", true, LocalDateTime.now());
        return new ApiResponse.AuthResponse("access-token", "refresh-token", "Bearer", 86400000L, userResp);
    }

    @Test
    @DisplayName("POST /api/auth/register — 201 Created on valid request")
    void register_success() throws Exception {
        when(authService.register(any())).thenReturn(mockAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest.RegisterRequest("Test User", "test@example.com", "Password1", "en"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /api/auth/register — 400 Bad Request on invalid email")
    void register_invalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest.RegisterRequest("Test", "not-an-email", "Password1", "en"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/register — 400 Bad Request on weak password")
    void register_weakPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest.RegisterRequest("Test", "test@example.com", "weak", "en"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login — 200 OK on valid credentials")
    void login_success() throws Exception {
        when(authService.login(any())).thenReturn(mockAuthResponse());

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest.LoginRequest("test@example.com", "Password1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("GET /api/auth/me — 200 OK for authenticated user")
    void getCurrentUser_authenticated() throws Exception {
        var userResp = new ApiResponse.UserResponse(
                UUID.randomUUID(), "Test User", "test@example.com",
                User.Role.USER, "en", true, LocalDateTime.now());
        when(authService.getCurrentUser("test@example.com")).thenReturn(userResp);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @DisplayName("GET /api/auth/me — 403 Forbidden for unauthenticated")
    void getCurrentUser_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }
}
