package com.example.user.core.filters;

import com.example.user.authentication.entities.SessionEntity;
import com.example.user.authentication.entities.TokenEntity;
import com.example.user.authentication.repository.TokenRepository;
import com.example.user.authentication.services.AuthenticationService;
import com.example.user.authentication.services.SessionService;
import com.example.user.authentication.services.TokenService;
import com.example.user.core.base.utils.ColorPrinter;
import com.example.user.core.base.utils.SetCookiesUtil;
import com.example.user.core.exceptions.TokenTerminatedException;
import com.example.user.core.security.JwtUtility;
import com.example.user.groups.entities.GroupEntity;
import com.example.user.users.entities.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private JwtUtility jwtUtility;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private Environment environment;

    @Autowired
    private TokenRepository tokenRepository;

    @Value("${app.jwt.access-cookie-name}")
    private String accessCookieName;

    @Value("${spring.application.jwt.access-token-lifetime}")
    private Integer accessJwtLifetime;

    @Autowired
    private SecretKey getSecretKey;

    private void validateSubjectSessionFromDb(UserDetails userDetails) {
        UserEntity userEntity = (UserEntity) userDetails;
        if (!sessionService.isUserSessionActive(userEntity)) {
            authenticationService.closeUserSession(userDetails);
            throw new JwtException("Session Expired");
        }
    }

    private void validateSubjectSessionFromDb(UserDetails userDetails, SessionEntity session) {
        UserEntity userEntity = (UserEntity) userDetails;
        if (!sessionService.isUserSessionActive(userEntity, session)) {
            authenticationService.closeUserSession(userDetails);
            throw new JwtException("Session Expired");
        }
    }

    private void loadSecurityContextOnValidSubject(String subject, HttpServletRequest request, HttpServletResponse response) {
        if (subject != null) {
            // Token is valid, hence  get the user details from username
            UserDetails userDetails = authenticationService.loadUserByUsername(subject);
            // Check whether the session in db is valid or not, else throw error and close all sessions
            SessionEntity session = sessionService.getLatestSessionByUsername(userDetails.getUsername());
            validateSubjectSessionFromDb(userDetails, session);

            // The session in db is valid, the token is valid, now we need to update the CURRENT token and
            // generate a new one if the time has exceeded touch interval.
            updateSessionWithNewToken(userDetails, session, response);
            // Now that the user session and token are valid, we set the authentication
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            ((UserEntity) auth.getPrincipal()).setPassword("");
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }

    private void updateSessionWithNewToken(UserDetails userDetails, SessionEntity session, HttpServletResponse response) {
        UserEntity user = (UserEntity) userDetails;

        // TODO: choose the better implementation for getting the latest token for session
        // TokenEntity latestTokenForSession = tokenService.getLatestTokenForSession((UserEntity) userDetails);
        TokenEntity latestTokenForSession = tokenService.getLatestTokenBySession_Id(session.getId());
        if (latestTokenForSession.getIsPrematureTerminated()) {
            throw new TokenTerminatedException("Your session has expired");
        }
        // Write update logic here
        long touchInterval = (long) Integer.parseInt(environment.getProperty("app.jwt.session.token.touch-after-interval", "2")) * 60;
        ColorPrinter.printColored("Touch Interval in updateSessionWithNewToken method:: " + String.valueOf(touchInterval), ColorPrinter.RED);
        if (
                new Date(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)).getTime() * 1000 - latestTokenForSession.getIat() >
                        touchInterval * 1000 &&
                        touchInterval * 1000 >
                                new Date(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)).getTime() * 1000 - latestTokenForSession.getExp()
        ) {
            latestTokenForSession.setIsPrematureTerminated(Boolean.TRUE);
            tokenRepository.save(latestTokenForSession);
            Map<String, Object> claims = Map.of("userId", user.getId(), "groups", user.getGroups() != null ? user.getGroups().stream().map(GroupEntity::getId).toList() : List.of());
            String generatedToken = authenticationService.generateToken(claims, user.getUsername());
            Claims newClaims = Jwts.parserBuilder().setSigningKey(getSecretKey).build().parseClaimsJws(generatedToken).getBody();
            TokenEntity newToken = TokenEntity
                    .builder()
                    .iat(newClaims.getIssuedAt()
                            .getTime())
                    .exp(newClaims.getExpiration().getTime())
                    .session(session)
                    .isPrematureTerminated(false)
                    .token(generatedToken)
                    .build();
            ColorPrinter.printColored(newToken.toString(), ColorPrinter.GREEN);
            tokenRepository.save(newToken);
            /*
            set-cookie
            JAccess=eyJUWVAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsImdyb3VwcyI6W10sInN1YiI6IlRlc3QxMjM0NSIsImlhdCI6MTc1MTIzMDE4NSwiZXhwIjoxNzUxMjMwMzY1fQ.VUgaGH2pSgV7ShV58UWurgJqJF3IlGA97YzJpQyyx1g;
             Path=/;
             Max-Age=180;
              Expires=Sun, 29 Jun 2025 15:24:09 GMT;
              Secure;
              HttpOnly;
                SameSite=Strict
             */
            ResponseCookie responseCookie = SetCookiesUtil.setCookie(accessCookieName, generatedToken, (accessJwtLifetime) * 60);
            response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
        }
    }

    private void authorizeViaAuthHeaders(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String rawToken = null;
        String jwt = null;
        String subject = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            rawToken = authHeader.substring(7); // "Bearer "
            Claims claims = jwtUtility.extractAllSignedClaims(rawToken).orElseThrow(() -> new ValidationException("Bad Token"));
            subject = claims.getSubject();
            System.out.println(subject);
        }
        loadSecurityContextOnValidSubject(subject, request, response);
    }

    private void authorizeViaCookies(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessCookieName = StringUtils.hasText(environment.getProperty("app.jwt.access-cookie-name")) ? environment.getProperty("app.jwt.access-cookie-name") : "JAccess";
        Optional<Cookie> authCookieValue = Arrays.stream(request.getCookies()).filter(x -> x.getName().equals(accessCookieName)).findFirst();
        String subject = null;

        if (authCookieValue.isPresent()) {
            Claims claims = jwtUtility.extractAllSignedClaims(authCookieValue.get().getValue()).orElseThrow(() -> new ValidationException("Bad Token"));
            subject = claims.getSubject();
        }
        loadSecurityContextOnValidSubject(subject, request, response);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        authorizeViaAuthHeaders(request, response, filterChain);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            authorizeViaCookies(request, response, filterChain);
        }
        filterChain.doFilter(request, response);
    }
}
