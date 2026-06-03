package com.healthcare.dto.request;

import jakarta.validation.constraints.*;

public class DoctorRequest {

    public record CreateDoctorRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100) String name,

            @NotBlank(message = "Specialization is required")
            @Size(max = 100) String specialization,

            @NotBlank(message = "Hospital is required")
            @Size(max = 150) String hospital,

            @NotBlank(message = "Location is required")
            @Size(max = 200) String location,

            @Size(max = 20) String phone,

            @Email @Size(max = 150) String email,

            Boolean isAvailable
    ) {}

    public record UpdateDoctorRequest(
            @Size(max = 100) String name,
            @Size(max = 100) String specialization,
            @Size(max = 150) String hospital,
            @Size(max = 200) String location,
            @Size(max = 20) String phone,
            @Email @Size(max = 150) String email,
            Boolean isAvailable,
            @DecimalMin("0.0") @DecimalMax("5.0") Double rating
    ) {}
}
