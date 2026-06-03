package com.healthcare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequest {

    public record SendMessageRequest(
            @NotBlank(message = "Message cannot be blank")
            @Size(min = 1, max = 5000, message = "Message must be between 1 and 5000 characters")
            String message,

            String sessionId,

            String language
    ) {}

    public record AnalyzeSymptomRequest(
            @NotBlank(message = "Symptoms description is required")
            @Size(min = 10, max = 5000, message = "Symptoms must be between 10 and 5000 characters")
            String symptoms,

            String severity,

            Integer age,

            String gender
    ) {}
}
