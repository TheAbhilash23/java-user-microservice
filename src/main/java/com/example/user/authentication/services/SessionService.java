package com.example.user.authentication.services;

import com.example.user.authentication.entities.SessionEntity;
import com.example.user.authentication.repository.SessionRepository;
import com.example.user.users.entities.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class SessionService {
    @Autowired
    private SessionRepository sessionRepository;

    @Value("${app.jwt.session.lifetime-hours}")
    private Integer sessionLifeTime;

    public SessionEntity createSession(UserEntity user) {
        SessionEntity session = SessionEntity
                .builder()
                .sessionStartTime(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .sessionEndTime(
                        LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + sessionLifeTime * 60 * 60)
                .user(user)
                .build();
        try {
            sessionRepository.save(session);
        } catch (DataAccessException e) {
            System.out.println("Retrying to save");
            sessionRepository.save(session);
        }
        return session;
    }

    /**
     * Takes UserEntity object and finds the latest session object from db and compares its
     * session end time with current time in epoch seconds.
     */
    public boolean isUserSessionActive(UserEntity user) {
        SessionEntity session = sessionRepository.findFirstByUserOrderBySessionStartTimeDesc(user);
        if (session == null) {
            return false;
        } else return session.getSessionEndTime() > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    }

    public boolean isUserSessionActive(UserEntity user, SessionEntity session) {
        if (session == null) {
            return false;
        } else return session.getSessionEndTime() > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    }

    public SessionEntity getLatestSessionByUsername(String username) {
        return sessionRepository.getLatestSessionByUsername(username, PageRequest.of(0, 1)).get(0);
    }

    public void closeUserSessions(Long userId, Long dateTime) {
        sessionRepository.closeAllUserSession(userId, dateTime);
        sessionRepository.closeAllUserSessionTokens(userId);
    }
}
