package com.healthcare.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class EmergencyDetectionService {

    private static final String EMERGENCY_MESSAGE =
            "⚠️ EMERGENCY ALERT: Your symptoms suggest a potentially life-threatening condition. " +
            "Please call emergency services (112 / 911) immediately or go to the nearest emergency room. " +
            "Do not delay seeking professional medical help.";

    @Value("${app.emergency.keywords:chest pain,breathing difficulty,unconscious,heavy bleeding," +
           "stroke,heart attack,seizure,overdose,cannot breathe,stopped breathing,severe chest," +
           "heart failure,anaphylaxis,severe allergic,suicidal,self harm,loss of consciousness," +
           "unresponsive,choking,severe burn,amputation,poisoning}")
    private String emergencyKeywordsConfig;

    private List<String> getKeywords() {
        return Arrays.stream(emergencyKeywordsConfig.toLowerCase().split(","))
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .toList();
    }

    public EmergencyResult detect(String text) {
        if (text == null || text.isBlank()) return EmergencyResult.safe();
        String lowerText = text.toLowerCase();
        List<String> matched = getKeywords().stream()
                .filter(lowerText::contains)
                .toList();
        if (!matched.isEmpty()) {
            log.warn("Emergency keywords detected: {}", matched);
            return EmergencyResult.emergency(matched, EMERGENCY_MESSAGE);
        }
        return EmergencyResult.safe();
    }

    public record EmergencyResult(boolean isEmergency, List<String> matchedKeywords, String emergencyMessage) {
        public static EmergencyResult safe() { return new EmergencyResult(false, List.of(), null); }
        public static EmergencyResult emergency(List<String> keywords, String message) {
            return new EmergencyResult(true, keywords, message);
        }
    }
}
