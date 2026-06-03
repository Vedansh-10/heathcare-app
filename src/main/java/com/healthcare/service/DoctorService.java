package com.healthcare.service;

import com.healthcare.dto.request.DoctorRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.entity.Doctor;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.healthcare.config.RedisConfig.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public ApiResponse.PagedResponse<ApiResponse.DoctorResponse> getAllDoctors(
            String specialization, String location, String query, Pageable pageable) {
        Page<Doctor> page;
        if (query != null && !query.isBlank()) {
            page = doctorRepository.searchDoctors(query.trim(), pageable);
        } else if (specialization != null && !specialization.isBlank()) {
            page = doctorRepository.findBySpecializationIgnoreCaseAndIsAvailableTrue(specialization, pageable);
        } else if (location != null && !location.isBlank()) {
            page = doctorRepository.findByLocationContainingIgnoreCaseAndIsAvailableTrue(location, pageable);
        } else {
            page = doctorRepository.findAll(pageable);
        }
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public ApiResponse.DoctorResponse getDoctorById(UUID id) {
        return mapToDoctorResponse(findById(id));
    }

    @Transactional
    @CacheEvict(value = CACHE_DOCTORS, allEntries = true)
    public ApiResponse.DoctorResponse createDoctor(DoctorRequest.CreateDoctorRequest request) {
        Doctor doctor = Doctor.builder()
                .name(request.name())
                .specialization(request.specialization())
                .hospital(request.hospital())
                .location(request.location())
                .phone(request.phone())
                .email(request.email())
                .isAvailable(request.isAvailable() != null ? request.isAvailable() : true)
                .build();
        doctor = doctorRepository.save(doctor);
        log.info("Doctor created: {}", doctor.getId());
        return mapToDoctorResponse(doctor);
    }

    @Transactional
    @CacheEvict(value = CACHE_DOCTORS, allEntries = true)
    public ApiResponse.DoctorResponse updateDoctor(UUID id, DoctorRequest.UpdateDoctorRequest request) {
        Doctor doctor = findById(id);
        if (request.name() != null)           doctor.setName(request.name());
        if (request.specialization() != null) doctor.setSpecialization(request.specialization());
        if (request.hospital() != null)       doctor.setHospital(request.hospital());
        if (request.location() != null)       doctor.setLocation(request.location());
        if (request.phone() != null)          doctor.setPhone(request.phone());
        if (request.email() != null)          doctor.setEmail(request.email());
        if (request.isAvailable() != null)    doctor.setIsAvailable(request.isAvailable());
        if (request.rating() != null)         doctor.setRating(request.rating());
        doctor = doctorRepository.save(doctor);
        log.info("Doctor updated: {}", id);
        return mapToDoctorResponse(doctor);
    }

    @Transactional
    @CacheEvict(value = CACHE_DOCTORS, allEntries = true)
    public void deleteDoctor(UUID id) {
        Doctor doctor = findById(id);
        doctorRepository.delete(doctor);
        log.info("Doctor deleted: {}", id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_DOCTORS, key = "'hospitals'")
    public List<String> getHospitals() {
        return doctorRepository.findDistinctHospitals();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_DOCTORS, key = "'specializations'")
    public List<String> getSpecializations() {
        return doctorRepository.findDistinctSpecializations();
    }

    @Transactional(readOnly = true)
    public List<ApiResponse.DoctorResponse> suggestDoctorsBySpecialization(String specialization) {
        if (specialization == null || specialization.isBlank()) return List.of();
        List<Doctor> doctors = doctorRepository.findTopBySpecializationForSuggestions(
                specialization, PageRequest.of(0, 3));
        return doctors.stream().map(this::mapToDoctorResponse).toList();
    }

    private Doctor findById(UUID id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
    }

    private ApiResponse.PagedResponse<ApiResponse.DoctorResponse> toPagedResponse(Page<Doctor> page) {
        List<ApiResponse.DoctorResponse> responses = page.getContent().stream()
                .map(this::mapToDoctorResponse).toList();
        return new ApiResponse.PagedResponse<>(
                responses, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    public ApiResponse.DoctorResponse mapToDoctorResponse(Doctor doctor) {
        return new ApiResponse.DoctorResponse(
                doctor.getId(), doctor.getName(), doctor.getSpecialization(),
                doctor.getHospital(), doctor.getLocation(), doctor.getPhone(),
                doctor.getEmail(), doctor.getIsAvailable(), doctor.getRating());
    }
}
