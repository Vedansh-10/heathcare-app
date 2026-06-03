package com.healthcare.controller;

import com.healthcare.dto.request.DoctorRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Doctors", description = "Doctor directory management")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/doctors")
    @Operation(summary = "List/search doctors with optional filters")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.PagedResponse<ApiResponse.DoctorResponse>>> getDoctors(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("name"));
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Doctors retrieved",
                doctorService.getAllDoctors(specialization, location, q, pageable)));
    }

    @GetMapping("/doctors/{id}")
    @Operation(summary = "Get doctor by ID")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.DoctorResponse>> getDoctorById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of(doctorService.getDoctorById(id)));
    }

    @PostMapping("/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new doctor (ADMIN only)")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.DoctorResponse>> createDoctor(
            @Valid @RequestBody DoctorRequest.CreateDoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.SuccessResponse.of("Doctor created", doctorService.createDoctor(request)));
    }

    @PutMapping("/doctors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a doctor (ADMIN only)")
    public ResponseEntity<ApiResponse.SuccessResponse<ApiResponse.DoctorResponse>> updateDoctor(
            @PathVariable UUID id,
            @Valid @RequestBody DoctorRequest.UpdateDoctorRequest request) {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Doctor updated", doctorService.updateDoctor(id, request)));
    }

    @DeleteMapping("/doctors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a doctor (ADMIN only)")
    public ResponseEntity<ApiResponse.SuccessResponse<Void>> deleteDoctor(@PathVariable UUID id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Doctor deleted", null));
    }

    @GetMapping("/hospitals")
    @Operation(summary = "List all distinct hospitals")
    public ResponseEntity<ApiResponse.SuccessResponse<List<String>>> getHospitals() {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Hospitals retrieved", doctorService.getHospitals()));
    }

    @GetMapping("/specializations")
    @Operation(summary = "List all distinct specializations")
    public ResponseEntity<ApiResponse.SuccessResponse<List<String>>> getSpecializations() {
        return ResponseEntity.ok(ApiResponse.SuccessResponse.of("Specializations retrieved", doctorService.getSpecializations()));
    }
}
