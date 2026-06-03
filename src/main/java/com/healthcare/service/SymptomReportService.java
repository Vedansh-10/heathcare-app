package com.healthcare.service;

import com.healthcare.ai.AiService;
import com.healthcare.dto.request.ChatRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.entity.SymptomReport;
import com.healthcare.entity.User;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.SymptomReportRepository;
import com.healthcare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SymptomReportService {

    private final SymptomReportRepository symptomReportRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final DoctorService doctorService;
    private final EmergencyDetectionService emergencyDetectionService;

    @Transactional
    public ApiResponse.AiAnalysisResponse analyzeAndSave(
            ChatRequest.AnalyzeSymptomRequest request, String userEmail) {

        User user = findUserByEmail(userEmail);

        // First get doctor suggestions based on symptoms keywords
        String specialization = guessSpecialization(request.symptoms());
        List<ApiResponse.DoctorResponse> suggestedDoctors = doctorService.suggestDoctorsBySpecialization(specialization);

        // Run AI analysis
        ApiResponse.AiAnalysisResponse analysis = aiService.analyzeSymptoms(request, suggestedDoctors);

        // Persist
        SymptomReport report = SymptomReport.builder()
                .user(user)
                .symptoms(request.symptoms())
                .severity(parseSeverity(request.severity()))
                .aiAnalysis(analysis.assessment())
                .homeRemedies(analysis.homeRemedies())
                .medicineSuggestions(analysis.medicineSuggestions())
                .recommendation(analysis.recommendation())
                .isEmergency(analysis.isEmergency())
                .build();

        symptomReportRepository.save(report);
        log.info("Symptom report saved for user [{}], emergency={}", userEmail, analysis.isEmergency());

        return analysis;
    }

    @Transactional(readOnly = true)
    public ApiResponse.PagedResponse<ApiResponse.SymptomReportResponse> getUserReports(
            String userEmail, Pageable pageable) {
        User user = findUserByEmail(userEmail);
        Page<SymptomReport> page = symptomReportRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        List<ApiResponse.SymptomReportResponse> content = page.getContent().stream()
                .map(this::mapToResponse).toList();
        return new ApiResponse.PagedResponse<>(
                content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Transactional(readOnly = true)
    public ApiResponse.SymptomReportResponse getReportById(UUID reportId, String userEmail) {
        User user = findUserByEmail(userEmail);
        SymptomReport report = symptomReportRepository.findByIdAndUserId(reportId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SymptomReport", "id", reportId));
        return mapToResponse(report);
    }

    @Transactional
    public void deleteReport(UUID reportId, String userEmail) {
        User user = findUserByEmail(userEmail);
        symptomReportRepository.findByIdAndUserId(reportId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SymptomReport", "id", reportId));
        symptomReportRepository.deleteByIdAndUserId(reportId, user.getId());
        log.info("Symptom report [{}] deleted for user [{}]", reportId, userEmail);
    }

    private String guessSpecialization(String symptoms) {
        if (symptoms == null) return "General Physician";
        String lower = symptoms.toLowerCase();
        if (lower.contains("heart") || lower.contains("chest") || lower.contains("cardiac")) return "Cardiologist";
        if (lower.contains("skin") || lower.contains("rash") || lower.contains("acne"))      return "Dermatologist";
        if (lower.contains("bone") || lower.contains("joint") || lower.contains("fracture"))  return "Orthopedist";
        if (lower.contains("brain") || lower.contains("neuro") || lower.contains("seizure"))  return "Neurologist";
        if (lower.contains("stomach") || lower.contains("digestion") || lower.contains("gastro")) return "Gastroenterologist";
        if (lower.contains("eye") || lower.contains("vision") || lower.contains("sight"))    return "Ophthalmologist";
        if (lower.contains("ear") || lower.contains("throat") || lower.contains("nose"))     return "ENT Specialist";
        if (lower.contains("lung") || lower.contains("breath") || lower.contains("cough"))   return "Pulmonologist";
        if (lower.contains("child") || lower.contains("infant") || lower.contains("baby"))   return "Pediatrician";
        return "General Physician";
    }

    private SymptomReport.Severity parseSeverity(String severity) {
        if (severity == null) return SymptomReport.Severity.MILD;
        try { return SymptomReport.Severity.valueOf(severity.toUpperCase()); }
        catch (IllegalArgumentException e) { return SymptomReport.Severity.MILD; }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private ApiResponse.SymptomReportResponse mapToResponse(SymptomReport r) {
        return new ApiResponse.SymptomReportResponse(
                r.getId(), r.getSymptoms(), r.getSeverity().name(),
                r.getAiAnalysis(), r.getHomeRemedies(), r.getMedicineSuggestions(),
                r.getRecommendation(), r.getIsEmergency(), r.getCreatedAt());
    }
}
