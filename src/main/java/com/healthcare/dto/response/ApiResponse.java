package com.healthcare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthcare.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    public record SuccessResponse<T>(boolean success, String message, T data) {
        public static <T> SuccessResponse<T> of(String message, T data) {
            return new SuccessResponse<>(true, message, data);
        }
        public static <T> SuccessResponse<T> of(T data) {
            return new SuccessResponse<>(true, "Operation successful", data);
        }
    }

    public record ErrorResponse(boolean success, String message, Object errors) {
        public static ErrorResponse of(String message) {
            return new ErrorResponse(false, message, null);
        }
        public static ErrorResponse of(String message, Object errors) {
            return new ErrorResponse(false, message, errors);
        }
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserResponse user
    ) {}

    public record UserResponse(
            UUID id,
            String name,
            String email,
            User.Role role,
            String language,
            Boolean isActive,
            LocalDateTime createdAt
    ) {}

    public record ChatResponse(
            UUID id,
            String message,
            String response,
            Boolean isEmergency,
            String emergencyMessage,
            String sessionId,
            LocalDateTime createdAt
    ) {}

    public record AiAnalysisResponse(
            String assessment,
            String homeRemedies,
            String medicineSuggestions,
            String recommendation,
            String severity,
            Boolean isEmergency,
            String emergencyMessage,
            List<DoctorResponse> suggestedDoctors
    ) {}

    public record DoctorResponse(
            UUID id,
            String name,
            String specialization,
            String hospital,
            String location,
            String phone,
            String email,
            Boolean isAvailable,
            Double rating
    ) {}

    public record SymptomReportResponse(
            UUID id,
            String symptoms,
            String severity,
            String aiAnalysis,
            String homeRemedies,
            String medicineSuggestions,
            String recommendation,
            Boolean isEmergency,
            LocalDateTime createdAt
    ) {}

    public record PagedResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {}

    public record MessageResponse(String message) {}
}