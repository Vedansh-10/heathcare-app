package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chats", indexes = {
        @Index(name = "idx_chat_user_id",    columnList = "user_id"),
        @Index(name = "idx_chat_session_id", columnList = "session_id"),
        @Index(name = "idx_chat_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(name = "is_emergency")
    @Builder.Default
    private Boolean isEmergency = false;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
