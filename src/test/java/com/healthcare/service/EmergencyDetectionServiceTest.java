package com.healthcare.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EmergencyDetectionService Unit Tests")
class EmergencyDetectionServiceTest {

    private EmergencyDetectionService service;

    @BeforeEach
    void setUp() {
        service = new EmergencyDetectionService();
        ReflectionTestUtils.setField(service, "emergencyKeywordsConfig",
                "chest pain,breathing difficulty,unconscious,heavy bleeding," +
                "stroke,heart attack,seizure,overdose,cannot breathe,suicidal,self harm");
    }

    @ParameterizedTest(name = "detects emergency keyword: {0}")
    @ValueSource(strings = {
            "I have severe chest pain",
            "Having trouble BREATHING DIFFICULTY",
            "Person is unconscious",
            "Heavy bleeding from wound",
            "Think I am having a stroke",
            "Symptoms of heart attack",
            "Patient having seizure",
            "Took overdose of medication",
            "Cannot breathe properly",
            "Feeling suicidal right now"
    })
    void detect_recognizesEmergencyKeywords(String input) {
        var result = service.detect(input);
        assertThat(result.isEmergency()).isTrue();
        assertThat(result.emergencyMessage()).contains("EMERGENCY ALERT");
        assertThat(result.matchedKeywords()).isNotEmpty();
    }

    @ParameterizedTest(name = "safe symptom: {0}")
    @ValueSource(strings = {
            "I have a mild headache",
            "Feeling tired after work",
            "Slight fever since yesterday",
            "Common cold symptoms",
            "Minor stomach ache"
    })
    void detect_safeSymptoms(String input) {
        var result = service.detect(input);
        assertThat(result.isEmergency()).isFalse();
        assertThat(result.emergencyMessage()).isNull();
        assertThat(result.matchedKeywords()).isEmpty();
    }

    @Test
    @DisplayName("detect() — handles null input safely")
    void detect_nullInput() {
        var result = service.detect(null);
        assertThat(result.isEmergency()).isFalse();
    }

    @Test
    @DisplayName("detect() — handles blank input safely")
    void detect_blankInput() {
        var result = service.detect("   ");
        assertThat(result.isEmergency()).isFalse();
    }

    @Test
    @DisplayName("detect() — case-insensitive matching")
    void detect_caseInsensitive() {
        var result = service.detect("CHEST PAIN and HEART ATTACK symptoms");
        assertThat(result.isEmergency()).isTrue();
        assertThat(result.matchedKeywords()).containsExactlyInAnyOrder("chest pain", "heart attack");
    }
}
