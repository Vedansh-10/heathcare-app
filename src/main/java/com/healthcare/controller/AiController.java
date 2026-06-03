package com.healthcare.controller;

import com.healthcare.dto.request.ChatRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.service.SymptomReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Analysis", description = "Symptom analysis and medical history")
public class AiController {

    private final SymptomReportService symptomReportService;

    @PostMapping("/analyze")
    @Operation(summary = "Analyze symptoms with AI",
               description = "Returns structured assessment, home remedies, medicine suggestions, " +
                             "emergency flag, and nearby doctor suggestions.")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.AiAnalysisResponse>> analyzeSymptoms(
            @Valid @RequestBody ChatRequest.AnalyzeSymptomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ApiResponse.AiAnalysisResponse analysis =
                symptomReportService.analyzeAndSave(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Symptom analysis completed", analysis));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get current user's symptom report history")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.PagedResponse<ApiResponse.SymptomReportResponse>>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Reports retrieved",
                symptomReportService.getUserReports(userDetails.getUsername(), pageable)));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get a specific symptom report by ID")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.SymptomReportResponse>> getReport(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of(
                symptomReportService.getReportById(id, userDetails.getUsername())));
    }

    @DeleteMapping("/reports/{id}")
    @Operation(summary = "Delete a specific symptom report")
    public ResponseEntity<ApiResponse.SuccessResponse<Void>> deleteReport(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        symptomReportService.deleteReport(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Report deleted", null));
    }
}
