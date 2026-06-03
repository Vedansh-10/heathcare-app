package com.healthcare.repository;

import com.healthcare.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByRefreshToken(String refreshToken);

    Page<User> findByRole(User.Role role, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.refreshToken = :token WHERE u.id = :id")
    void updateRefreshToken(@Param("id") UUID id, @Param("token") String token);

    @Modifying
    @Query("UPDATE User u SET u.refreshToken = null WHERE u.email = :email")
    void clearRefreshToken(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.role = :role")
    Page<User> findActiveUsersByRole(@Param("role") User.Role role, Pageable pageable);
}
