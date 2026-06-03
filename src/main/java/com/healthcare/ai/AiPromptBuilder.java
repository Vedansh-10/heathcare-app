package com.healthcare.ai;

import org.springframework.stereotype.Component;

@Component
public class AiPromptBuilder {

    public String buildSymptomAnalysisPrompt(String symptoms, String severity, Integer age, String gender) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a compassionate AI Healthcare Assistant. Analyze the patient's symptoms and respond ONLY in the following JSON format (no markdown, no extra text):\n\n");
        sb.append("{\n");
        sb.append("  \"assessment\": \"<2-3 sentence assessment, NOT a diagnosis>\",\n");
        sb.append("  \"homeRemedies\": \"<specific home remedies the patient can try>\",\n");
        sb.append("  \"medicineSuggestions\": \"<safe OTC medicines if applicable, or 'Consult a doctor for medication'>\",\n");
        sb.append("  \"recommendation\": \"<clear next steps and precautions>\",\n");
        sb.append("  \"suggestedSpecialization\": \"<one medical specialization most relevant, e.g. General Physician, Cardiologist>\"\n");
        sb.append("}\n\n");
        sb.append("PATIENT INFORMATION:\n");
        if (age != null)      sb.append("- Age: ").append(age).append("\n");
        if (gender != null)   sb.append("- Gender: ").append(gender).append("\n");
        if (severity != null) sb.append("- Reported severity: ").append(severity).append("\n");
        sb.append("\nSYMPTOMS:\n").append(symptoms);
        sb.append("\n\nIMPORTANT: Always recommend consulting a doctor for serious or persistent symptoms. Do NOT prescribe specific dosages.");
        return sb.toString();
    }

    public String buildGeneralHealthPrompt(String userMessage, String language) {
        return String.format("User message (language preference: %s):\n%s",
                language != null ? language : "en", userMessage);
    }

    public String buildConversationPrompt(String userMessage, String conversationHistory, String language) {
        StringBuilder sb = new StringBuilder();
        if (conversationHistory != null && !conversationHistory.isBlank()) {
            sb.append("Previous conversation:\n").append(conversationHistory).append("\n\n");
        }
        sb.append("Current message (language: ").append(language != null ? language : "en").append("):\n");
        sb.append(userMessage);
        return sb.toString();
    }
}
