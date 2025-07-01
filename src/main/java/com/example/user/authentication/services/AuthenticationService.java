package com.example.user.authentication.services;

import com.example.user.authentication.entities.SessionEntity;
import com.example.user.authentication.entities.TokenEntity;
import com.example.user.authentication.repository.TokenRepository;
import com.example.user.core.base.utils.ColorPrinter;
import com.example.user.core.exceptions.SessionStillActiveException;
import com.example.user.core.security.JwtAuthInterface;
import com.example.user.groups.entities.GroupEntity;
import com.example.user.users.dto.UserAuthenticationDto;
import com.example.user.users.dto.UserPermissionsDto;
import com.example.user.users.entities.UserEntity;
import com.example.user.users.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthenticationService implements UserDetailsService, JwtAuthInterface {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${spring.application.secret-key-jwt}")
    private String SECRET_KEY;

    @Value("${spring.application.jwt.access-token-lifetime}")
    private Integer ACCESS_TOKEN_LIFETIME;

    @Bean
    private SecretKey getSecretKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(); // based on key length the algorithm will be decided.
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public final SessionEntity createUserSession(UserEntity user) throws SessionStillActiveException {
        // create  a sessions entity in DB
        return sessionService.createSession(user);
    }


    /*
        Takes user credentials from UserAthenticationDto, validates and creates session and token
        and returns token.
     */
    @Override
    public String loginViaCredentials(UserAuthenticationDto userAuthenticationDto) {
        AuthenticationManager authenticationManager = applicationContext.getBean(AuthenticationManager.class);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userAuthenticationDto.getUsername(),
                        userAuthenticationDto.getPassword()));
        // Get User
        UserDetails userDetails = loadUserByUsername(userAuthenticationDto.getUsername());
        /* Check Whether this user has an active session, if it is active:-
          1. Then, either this user a hostile user, or the active session is of the hostile user
          2. In this case, close user's session
          3. Disable user from logging in till admin is involved.
        */
        UserEntity userEntity = (UserEntity) userDetails;
        if (sessionService.isUserSessionActive(userEntity)) {
            ColorPrinter.printColored("User Session is ACTIVE", ColorPrinter.RED);
            closeUserSession(userDetails);
            userEntity.setIsEnabled(false);
            userRepository.save(userEntity);
            throw new SessionStillActiveException("Your session was active on another machine, hence your account is " +
                    "temporarily blocked for access. Contact Admin for further actions");
        }
        // no active Session,
        // 1. begin by creating a session
        // 2. create session's token
        SessionEntity userSession = createUserSession(userEntity);

        Map<String, Object> claims = Map.of(
                "userId", userEntity.getId(),
                "groups", userEntity.getGroups() != null ? userEntity.getGroups()
                        .stream()
                        .map(GroupEntity::getId)
                        .toList() : List.of()
        );
        return createSessionToken(claims, userSession);
    }


    private String createToken(Map<String, Object> claims, String username) {
        String ret =  Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setHeader(Map.of("TYP", "JWT"))
                .setIssuedAt(new Date(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000))
                .setExpiration(new Date(
                        LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000
                                + (long) (ACCESS_TOKEN_LIFETIME) * 60 * 1000))
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
        ColorPrinter.printColored(ret, ColorPrinter.GREEN);
        return ret;
    }

    public String generateToken(Map<String, Object> claims, String username) {
        if (claims == null) {
            claims = new HashMap<>();
        }
        return createToken(claims, username);
    }

    /**
     * After creating user session, invalidate other tokens,
     */
    @Override
    public String createSessionToken(Map<String, Object> claims, SessionEntity session) {

        String generatedToken = generateToken(claims, session.getUser().getUsername());
        // no need to verify the claims since token is generated locally
        Claims generatedTokenClaims = Jwts
                .parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(generatedToken)
                .getBody();
        TokenEntity tokenEntity = TokenEntity
                .builder()
                .iat(generatedTokenClaims.getIssuedAt().getTime())
                .exp(generatedTokenClaims.getExpiration().getTime())
                .session(session)
                .isPrematureTerminated(false)
                .token(generatedToken)
                .build();
        ColorPrinter.printColored(tokenEntity.toString(), ColorPrinter.RED);
        tokenRepository.save(tokenEntity);
        return generatedToken;
    }

    @Override
    public String getSessionByToken(String token) {
        return "";
    }

    @Override
    public Optional<Claims> extractAllSignedClaims(String rawToken) {
        try {
            JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(getSecretKey()).build();
            Claims claims = jwtParser.parseClaimsJws(rawToken).getBody();
            if (claims.getExpiration().before(new Date())) {
                throw new JwtException("Token Expired get a new token");
            }
            return Optional.of(claims);
        } catch (JwtException e) {
            System.out.println("Bad Token");
            return Optional.empty();
        }
    }

    @Override
    public void closeUserSession(UserDetails userDetails) {
        UserEntity userEntity = (UserEntity) userDetails;
        sessionService.closeUserSessions(userEntity.getId(), LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        UserEntity user = UserEntity.builder()
                .id(userEntity.getId())
                .username(userEntity.getUsername())
                .password(userEntity.getPassword())
                .email(userEntity.getEmail())
                .isEnabled(userEntity.isEnabled())
                .firstName(userEntity.getFirstName())
                .lastName(userEntity.getLastName())
                .roles(userEntity.getRoles())
                .build();
        user.setRoles(getUserRoles(user.getId()));
        return user;
    }

    public Set<String> getUserRoles(Long id) {
        List<UserPermissionsDto> userPermissions = userRepository.getUserPermissions(id);
        return userPermissions
                .stream()
                .map(x -> x.getGroupName() + "," + x.getPermissionName() + "," + x.getModuleName())
                .collect(Collectors.toSet());
    }
}
