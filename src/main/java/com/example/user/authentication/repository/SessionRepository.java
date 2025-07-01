package com.example.user.authentication.repository;

import com.example.user.authentication.entities.SessionEntity;
import com.example.user.users.entities.UserEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {

    List<SessionEntity> getSessionsByUser(UserEntity user);

    SessionEntity findFirstByUserOrderBySessionStartTimeDesc(UserEntity user);

    @Query("""
            SELECT s
             FROM SessionEntity s
             JOIN s.user u
             WHERE u.username = :username
             ORDER BY s.sessionStartTime DESC
            """)
    List<SessionEntity> getLatestSessionByUsername(@Param("username") String username, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            UPDATE SessionEntity s
            SET
             s.sessionEndTime = :dateTime
            WHERE
             s.user.id = :userId
            """)
    void closeAllUserSession(@Param("userId") Long userId, @Param("dateTime") Long dateTime);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
            UPDATE com.example.user.authentication.entities.TokenEntity t
            SET
             t.isPrematureTerminated = TRUE
            WHERE
             t.session.user.id = :userId
            """)
    void closeAllUserSessionTokens(@Param("userId") Long userId);
}
