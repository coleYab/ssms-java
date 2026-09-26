package com.example.ssms.auth.repository;

import com.example.ssms.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    List<UserSession> findByUserIdAndRevokedFalseOrderByLastUsedAtDesc(UUID userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.userId = :userId")
    void revokeAllByUserId(UUID userId);

    long countByUserIdAndRevokedFalse(UUID userId);
}
