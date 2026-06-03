package com.healthcare.repository;

import com.healthcare.entity.SymptomReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SymptomReportRepository extends JpaRepository<SymptomReport, UUID> {

    Page<SymptomReport> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<SymptomReport> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT s FROM SymptomReport s WHERE s.user.id = :userId AND s.isEmergency = true ORDER BY s.createdAt DESC")
    Page<SymptomReport> findEmergencyReportsByUserId(@Param("userId") UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndSeverity(UUID userId, SymptomReport.Severity severity);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
