package com.healthcare.service;

import com.healthcare.dto.request.DoctorRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.entity.Doctor;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorService Unit Tests")
class DoctorServiceTest {

    @Mock DoctorRepository doctorRepository;
    @InjectMocks DoctorService doctorService;

    private Doctor testDoctor;

    @BeforeEach
    void setUp() {
        testDoctor = Doctor.builder()
                .id(UUID.randomUUID())
                .name("Dr. Smith")
                .specialization("General Physician")
                .hospital("City Hospital")
                .location("Mumbai")
                .phone("+91-9999999999")
                .isAvailable(true)
                .rating(4.5)
                .build();
    }

    @Test
    @DisplayName("getAllDoctors() — returns paged response")
    void getAllDoctors_noFilters() {
        when(doctorRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(testDoctor)));

        var result = doctorService.getAllDoctors(null, null, null, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Dr. Smith");
    }

    @Test
    @DisplayName("getAllDoctors() — uses search query when provided")
    void getAllDoctors_withQuery() {
        when(doctorRepository.searchDoctors(eq("Smith"), any()))
                .thenReturn(new PageImpl<>(List.of(testDoctor)));

        var result = doctorService.getAllDoctors(null, null, "Smith", PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        verify(doctorRepository).searchDoctors(eq("Smith"), any());
    }

    @Test
    @DisplayName("getDoctorById() — throws ResourceNotFoundException for unknown id")
    void getDoctorById_notFound() {
        UUID id = UUID.randomUUID();
        when(doctorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.getDoctorById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("createDoctor() — saves and returns doctor response")
    void createDoctor_success() {
        var request = new DoctorRequest.CreateDoctorRequest(
                "Dr. Jones", "Cardiologist", "Heart Center", "Delhi",
                "+91-8888888888", "jones@hospital.com", true);

        when(doctorRepository.save(any())).thenReturn(testDoctor);

        ApiResponse.DoctorResponse result = doctorService.createDoctor(request);

        assertThat(result).isNotNull();
        verify(doctorRepository).save(any(Doctor.class));
    }

    @Test
    @DisplayName("updateDoctor() — updates only provided fields")
    void updateDoctor_partialUpdate() {
        UUID id = testDoctor.getId();
        when(doctorRepository.findById(id)).thenReturn(Optional.of(testDoctor));
        when(doctorRepository.save(any())).thenReturn(testDoctor);

        var request = new DoctorRequest.UpdateDoctorRequest(
                null, null, null, null, null, null, false, null);

        doctorService.updateDoctor(id, request);

        verify(doctorRepository).save(argThat(d -> !d.getIsAvailable()));
    }

    @Test
    @DisplayName("deleteDoctor() — removes doctor from repository")
    void deleteDoctor_success() {
        UUID id = testDoctor.getId();
        when(doctorRepository.findById(id)).thenReturn(Optional.of(testDoctor));
        doNothing().when(doctorRepository).delete(any());

        doctorService.deleteDoctor(id);

        verify(doctorRepository).delete(testDoctor);
    }

    @Test
    @DisplayName("getHospitals() — delegates to repository")
    void getHospitals() {
        when(doctorRepository.findDistinctHospitals()).thenReturn(List.of("City Hospital", "General Hospital"));

        List<String> result = doctorService.getHospitals();

        assertThat(result).containsExactly("City Hospital", "General Hospital");
    }
}
