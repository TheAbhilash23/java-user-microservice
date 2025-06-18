package com.example.user.authentication.services;

import com.example.user.authentication.repository.SessionRepository;
import com.example.user.authentication.repository.TokenRepository;
import com.example.user.core.security.JwtAuthInterface;
import com.example.user.users.entities.UserEntity;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthenticationService implements JwtAuthInterface {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private TokenRepository tokenRepository;


    @Override
    public SecretKey getSecretKey() {
        return null;
    }

    @Override
    public void createUserSession(UserEntity user) {

    }

    @Override
    public String createSessionToken(Map<String, Object> claims, String username) {
        return "";
    }

    @Override
    public String getSessionByToken(String token) {
        return "";
    }

    @Override
    public Optional<Claims> extractAllSignedClaims(String rawToken) {
        return Optional.empty();
    }

    @Override
    public void closeUserSession(String token) {

    }
}
