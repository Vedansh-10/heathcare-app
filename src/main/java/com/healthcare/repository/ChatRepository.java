package com.healthcare.repository;

import com.healthcare.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    Page<Chat> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Chat> findByIdAndUserId(UUID id, UUID userId);

    List<Chat> findByUserIdAndSessionIdOrderByCreatedAtAsc(UUID userId, String sessionId);

    // Alias used in ChatService
    default List<Chat> findByUserIdAndSessionId(UUID userId, String sessionId) {
        return findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId);
    }

    List<Chat> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("DELETE FROM Chat c WHERE c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    long countByUserId(UUID userId);
}
