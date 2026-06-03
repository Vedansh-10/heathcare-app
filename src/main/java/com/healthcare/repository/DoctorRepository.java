package com.healthcare.repository;

import com.healthcare.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    Page<Doctor> findBySpecializationIgnoreCaseAndIsAvailableTrue(String specialization, Pageable pageable);

    Page<Doctor> findByLocationContainingIgnoreCaseAndIsAvailableTrue(String location, Pageable pageable);

    @Query("SELECT DISTINCT d.hospital FROM Doctor d WHERE d.isAvailable = true ORDER BY d.hospital")
    List<String> findDistinctHospitals();

    @Query("SELECT d FROM Doctor d WHERE d.isAvailable = true AND " +
           "(LOWER(d.specialization) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.hospital) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Doctor> searchDoctors(@Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT d.specialization FROM Doctor d ORDER BY d.specialization")
    List<String> findDistinctSpecializations();

    @Query("SELECT d FROM Doctor d WHERE d.isAvailable = true AND " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :specialization, '%')) " +
           "ORDER BY d.rating DESC NULLS LAST")
    List<Doctor> findTopBySpecializationForSuggestions(@Param("specialization") String specialization, Pageable pageable);
}
