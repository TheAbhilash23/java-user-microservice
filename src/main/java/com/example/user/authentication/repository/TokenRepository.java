package com.example.user.authentication.repository;

import com.example.user.authentication.entities.TokenEntity;
import com.example.user.users.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepository extends JpaRepository<TokenEntity, Long> {
    TokenEntity findFirstBySession_UserAndIsPrematureTerminatedFalseOrderBySession_SessionStartTimeDescIatDesc(UserEntity user);
    TokenEntity findFirstBySession_IdOrderByIatDesc(String sessionId);
}
