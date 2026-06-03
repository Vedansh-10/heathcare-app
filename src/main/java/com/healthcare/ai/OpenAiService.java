package com.healthcare.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.dto.request.ChatRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.service.EmergencyDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService implements AiService {

    private final ChatClient.Builder chatClientBuilder;
    private final EmergencyDetectionService emergencyDetectionService;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a compassionate, knowledgeable AI Healthcare Assistant.
            
            GUIDELINES:
            1. Always recommend consulting a licensed healthcare professional for diagnosis and treatment.
            2. For emergencies (chest pain, difficulty breathing, stroke symptoms, etc.), immediately urge calling emergency services.
            3. Provide evidence-based, accurate health information.
            4. Be empathetic and supportive.
            5. Do NOT provide specific dosage recommendations or prescribe medications.
            6. Keep responses clear, concise, and easy to understand.
            7. Ask follow-up questions when symptoms are unclear to give better guidance.
            8. If asked non-health questions, gently redirect to health topics.
            """;

    @Override
    public String chat(String userMessage, String conversationHistory, String language) {
        try {
            String systemContent = buildLocalizedSystem(language);
            String prompt = promptBuilder.buildConversationPrompt(userMessage, conversationHistory, language);
            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                    .messages(new SystemMessage(systemContent), new UserMessage(prompt))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI chat failed: {}", e.getMessage(), e);
            return "I'm sorry, I'm unable to process your request at the moment. " +
                   "Please try again later or consult a healthcare professional directly.";
        }
    }

    @Override
    public ApiResponse.AiAnalysisResponse analyzeSymptoms(ChatRequest.AnalyzeSymptomRequest request,
                                                           List<ApiResponse.DoctorResponse> suggestedDoctors) {
        EmergencyDetectionService.EmergencyResult emergency = emergencyDetectionService.detect(request.symptoms());

        String prompt = promptBuilder.buildSymptomAnalysisPrompt(
                request.symptoms(), request.severity(), request.age(), request.gender());

        try {
            ChatClient chatClient = chatClientBuilder.build();
            String aiResponse = chatClient.prompt()
                    .messages(new SystemMessage(SYSTEM_PROMPT), new UserMessage(prompt))
                    .call()
                    .content();

            return parseAnalysisResponse(aiResponse, request.severity(), emergency, suggestedDoctors);
        } catch (Exception e) {
            log.error("Symptom analysis failed: {}", e.getMessage(), e);
            return new ApiResponse.AiAnalysisResponse(
                    "Unable to analyze symptoms at this time.",
                    "Rest, hydrate, and monitor your symptoms.",
                    "Consult a doctor for medication advice.",
                    "Please consult a healthcare professional.",
                    request.severity() != null ? request.severity() : "UNKNOWN",
                    emergency.isEmergency(),
                    emergency.isEmergency() ? emergency.emergencyMessage() : null,
                    suggestedDoctors
            );
        }
    }

    @Override
    public String buildHealthcarePrompt(String userMessage, String language) {
        return promptBuilder.buildGeneralHealthPrompt(userMessage, language);
    }

    private ApiResponse.AiAnalysisResponse parseAnalysisResponse(
            String aiResponse, String severity,
            EmergencyDetectionService.EmergencyResult emergency,
            List<ApiResponse.DoctorResponse> suggestedDoctors) {

        String assessment = aiResponse;
        String homeRemedies = "Rest, stay hydrated, and monitor your symptoms.";
        String medicineSuggestions = "Consult a doctor for appropriate medication.";
        String recommendation = "Please consult a healthcare professional for personalized advice.";

        try {
            String json = aiResponse.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```json\\s*|```\\s*", "").trim();
            }
            JsonNode node = objectMapper.readTree(json);
            assessment        = getTextOrDefault(node, "assessment", assessment);
            homeRemedies      = getTextOrDefault(node, "homeRemedies", homeRemedies);
            medicineSuggestions = getTextOrDefault(node, "medicineSuggestions", medicineSuggestions);
            recommendation    = getTextOrDefault(node, "recommendation", recommendation);
        } catch (Exception e) {
            log.warn("Could not parse AI JSON response, using raw text: {}", e.getMessage());
        }

        return new ApiResponse.AiAnalysisResponse(
                assessment, homeRemedies, medicineSuggestions, recommendation,
                severity != null ? severity : "UNKNOWN",
                emergency.isEmergency(),
                emergency.isEmergency() ? emergency.emergencyMessage() : null,
                suggestedDoctors
        );
    }

    private String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull() && !n.asText().isBlank()) ? n.asText() : defaultValue;
    }

    private String buildLocalizedSystem(String language) {
        if (language == null) return SYSTEM_PROMPT;
        return switch (language.toLowerCase()) {
            case "hi" -> SYSTEM_PROMPT + "\nRespond in Hindi (हिंदी) unless the user writes in another language.";
            case "es" -> SYSTEM_PROMPT + "\nRespond in Spanish unless the user writes in another language.";
            case "fr" -> SYSTEM_PROMPT + "\nRespond in French unless the user writes in another language.";
            case "de" -> SYSTEM_PROMPT + "\nRespond in German unless the user writes in another language.";
            case "ar" -> SYSTEM_PROMPT + "\nRespond in Arabic unless the user writes in another language.";
            case "zh" -> SYSTEM_PROMPT + "\nRespond in Chinese (Mandarin) unless the user writes in another language.";
            case "pt" -> SYSTEM_PROMPT + "\nRespond in Portuguese unless the user writes in another language.";
            default   -> SYSTEM_PROMPT;
        };
    }
}
