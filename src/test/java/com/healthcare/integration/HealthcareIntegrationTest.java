package com.healthcare.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.dto.request.AuthRequest;
import com.healthcare.dto.request.ChatRequest;
import com.healthcare.dto.response.ApiResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests: boots the full context with H2 (in-memory DB).
 * The OpenAI ChatClient is mocked so no real API calls are made.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Healthcare Integration Tests")
class HealthcareIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ChatClient.Builder chatClientBuilder;

    private static String accessToken;

    @BeforeEach
    void mockAi() {
        var mockCall = org.mockito.Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        var mockCallResponse = org.mockito.Mockito.mock(ChatClient.CallResponseSpec.class);
        when(chatClientBuilder.build()).thenReturn(org.mockito.Mockito.mock(ChatClient.class));

        ChatClient mockClient = chatClientBuilder.build();
        when(mockClient.prompt()).thenReturn(mockCall);
        when(mockCall.messages(any())).thenReturn(mockCall);
        when(mockCall.call()).thenReturn(mockCallResponse);
        when(mockCallResponse.content()).thenReturn("{"
                + "\"assessment\":\"You may have a common cold.\","
                + "\"homeRemedies\":\"Rest and drink fluids.\","
                + "\"medicineSuggestions\":\"Consult a doctor.\","
                + "\"recommendation\":\"Rest for 2 days.\","
                + "\"suggestedSpecialization\":\"General Physician\""
                + "}");
    }

    @Test
    @Order(1)
    @DisplayName("Register new user — 201 Created")
    void test_register() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest.RegisterRequest(
                                        "Integration User", "integration@test.com", "Password1", "en"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        var response = objectMapper.readTree(body);
        accessToken = response.path("data").path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("Login existing user — 200 OK with tokens")
    void test_login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest.LoginRequest("integration@test.com", "Password1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).path("data").path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/auth/me — returns current user")
    void test_getCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("integration@test.com"));
    }

    @Test
    @Order(4)
    @DisplayName("Duplicate register — 409 Conflict")
    void test_duplicateRegister() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest.RegisterRequest(
                                        "Dup User", "integration@test.com", "Password1", "en"))))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/doctors — returns paginated doctor list")
    void test_getDoctors() throws Exception {
        mockMvc.perform(get("/api/doctors")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/specializations — returns list")
    void test_getSpecializations() throws Exception {
        mockMvc.perform(get("/api/specializations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(7)
    @DisplayName("Unauthenticated request returns 403")
    void test_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/ai/reports — returns empty list initially")
    void test_getReportsInitial() throws Exception {
        mockMvc.perform(get("/api/ai/reports")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Order(9)
    @DisplayName("PUT /api/auth/me — updates user profile")
    void test_updateProfile() throws Exception {
        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthRequest.UpdateProfileRequest("Updated Name", "hi"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.language").value("hi"));
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/auth/logout — clears token")
    void test_logout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
