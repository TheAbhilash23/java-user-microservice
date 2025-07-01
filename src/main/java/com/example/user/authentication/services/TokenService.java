package com.example.user.authentication.services;

import com.example.user.authentication.entities.SessionEntity;
import com.example.user.authentication.entities.TokenEntity;
import com.example.user.authentication.repository.TokenRepository;
import com.example.user.users.entities.UserEntity;
import org.antlr.v4.runtime.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TokenService {

    @Autowired
    private TokenRepository tokenRepository;

    public Long createToken(TokenEntity token){
        TokenEntity tokenEntity = tokenRepository.save(token);
        return tokenEntity.getId();
    }

    public TokenEntity getLatestTokenForUser(UserEntity user){
        return tokenRepository.findFirstBySession_UserAndIsPrematureTerminatedFalseOrderBySession_SessionStartTimeDescIatDesc(user);
    }
    
    public Optional<TokenEntity> getTokenById(Long tokenId){
        return tokenRepository.findById(tokenId);
    }

    public TokenEntity getLatestTokenBySession_Id(String sessionId){
        return tokenRepository.findFirstBySession_IdOrderByIatDesc(sessionId);
    }

//    public Long createTokenForUserSession(SessionEntity session){
//        TokenEntity
//                .builder()
//                .session()
////        TokenEntity tokenEntity = tokenRepository.save(token);
//        return tokenEntity.getId();
//    }
}
