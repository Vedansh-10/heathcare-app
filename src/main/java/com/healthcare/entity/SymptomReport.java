package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "symptom_reports", indexes = {
        @Index(name = "idx_symptom_user_id", columnList = "user_id"),
        @Index(name = "idx_symptom_severity", columnList = "severity"),
        @Index(name = "idx_symptom_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SymptomReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String symptoms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Severity severity = Severity.MILD;

    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column(name = "home_remedies", columnDefinition = "TEXT")
    private String homeRemedies;

    @Column(name = "medicine_suggestions", columnDefinition = "TEXT")
    private String medicineSuggestions;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "is_emergency")
    @Builder.Default
    private Boolean isEmergency = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Severity {
        MILD, MODERATE, SEVERE, CRITICAL
    }
}
