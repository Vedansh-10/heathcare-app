package com.healthcare.ai;

import com.healthcare.dto.request.ChatRequest;
import com.healthcare.dto.response.ApiResponse;

import java.util.List;

public interface AiService {
    String chat(String userMessage, String conversationHistory, String language);
    ApiResponse.AiAnalysisResponse analyzeSymptoms(ChatRequest.AnalyzeSymptomRequest request,
                                                    List<ApiResponse.DoctorResponse> suggestedDoctors);
    String buildHealthcarePrompt(String userMessage, String language);
}
