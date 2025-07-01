package com.example.user.core.security;

import com.example.user.authentication.entities.SessionEntity;
import com.example.user.users.dto.UserAuthenticationDto;
import com.example.user.users.entities.UserEntity;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.Optional;

public interface JwtAuthInterface {

    SessionEntity createUserSession(UserEntity user);

    String createSessionToken(Map<String, Object> claims, SessionEntity session);

    String getSessionByToken(String token);

    Optional<Claims> extractAllSignedClaims(String rawToken);

    void closeUserSession(UserDetails userDetails);

    String loginViaCredentials(UserAuthenticationDto userAuthenticationDto);
}
