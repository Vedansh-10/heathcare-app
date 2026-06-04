package com.healthcare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.config.RateLimitConfig;
import com.healthcare.config.SecurityConfig;
import com.healthcare.dto.request.ChatRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.security.JwtAuthenticationFilter;
import com.healthcare.security.JwtService;
import com.healthcare.security.UserDetailsServiceImpl;
import com.healthcare.service.ChatService;
import com.healthcare.util.RateLimitFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
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
        controllers = ChatController.class,
//        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {RateLimitConfig.class, RateLimitFilter.class, SecurityConfig.class, JwtAuthenticationFilter.class})
)

@MockBean(JpaMetamodelMappingContext.class)
@DisplayName("ChatController Web Layer Tests")
class ChatControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ChatService chatService;
    @MockBean JwtService jwtService;
    @MockBean
    UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("POST /api/chat/message — 200 OK with AI response")
    void sendMessage_success() throws Exception {
        var chatResponse = new ApiResponse.ChatResponse(
                UUID.randomUUID(), "I have a headache",
                "Rest and stay hydrated.", false, null, "sess-1", LocalDateTime.now());

        when(chatService.sendMessage(any(), eq("test@example.com"))).thenReturn(chatResponse);

        mockMvc.perform(post("/api/chat/message")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChatRequest.SendMessageRequest("I have a headache", "sess-1", "en"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("I have a headache"))
                .andExpect(jsonPath("$.data.isEmergency").value(false));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("POST /api/chat/message — 400 on blank message")
    void sendMessage_blankMessage() throws Exception {
        mockMvc.perform(post("/api/chat/message")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChatRequest.SendMessageRequest("", null, "en"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("GET /api/chat/history — 200 with paginated results")
    void getChatHistory_success() throws Exception {
        var chatResponse = new ApiResponse.ChatResponse(
                UUID.randomUUID(), "test", "response", false, null, null, LocalDateTime.now());
        var paged = new ApiResponse.PagedResponse<>(List.of(chatResponse), 0, 20, 1L, 1, true);

        when(chatService.getChatHistory(eq("test@example.com"), any())).thenReturn(paged);

        mockMvc.perform(get("/api/chat/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("DELETE /api/chat/{id} — 200 on successful deletion")
    void deleteChat_success() throws Exception {
        UUID chatId = UUID.randomUUID();
        doNothing().when(chatService).deleteChat(any(), any());

        mockMvc.perform(delete("/api/chat/" + chatId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/chat/message — 403 Forbidden without authentication")
    void sendMessage_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/chat/message")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
