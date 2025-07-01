package com.example.user.authentication.controllers;

import com.example.user.authentication.services.AuthenticationService;
import com.example.user.core.base.utils.SetCookiesUtil;
import com.example.user.core.security.JwtUtility;
import com.example.user.users.dto.UserAuthenticationDto;
import com.example.user.users.dto.UserDto;
import com.example.user.users.entities.UserEntity;
import com.example.user.users.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/authentication")
@Tag(name = "Authentication", description = "APIs for managing authentication.")
public class AuthenticationController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtility jwtUtility;

    @Autowired
    private Environment environment;

    @Autowired
    private AuthenticationService authenticationService;

    //    @Operation(summary = "Authenticate using user credentials", description = "Returns refresh and access tokens.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Successfully logged in."),
//            @ApiResponse(responseCode = "400", description = "Invalid credentials")
//    })
    @PostMapping("login/")
    public ResponseEntity<Map<String, String>> authenticate(@RequestBody UserAuthenticationDto userDto) {
        try {
            String access = authenticationService.loginViaCredentials(userDto);
            String accessCookieName = StringUtils.hasText(environment.getProperty("app.jwt.access-cookie-name")) ? environment.getProperty("app.jwt.access-cookie-name") : "JAccess";
            int accessJwtLifetime = Integer.parseInt(StringUtils.hasText(environment.getProperty("spring.application.jwt.access-token-lifetime")) ? environment.getProperty("spring.application.jwt.access-token-lifetime") : "5");
            // Set cookies now
            ResponseCookie responseCookie = SetCookiesUtil.setCookie(accessCookieName, access, (accessJwtLifetime) * 60);
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                    .body(Map.of("token", access));

        } catch (Exception e) {
            System.out.println("error occurred:- ");
            for (StackTraceElement trace : e.getStackTrace()) {
                System.out.println(trace.toString());
            }
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("detail", "Bad Credentials provided."));
        }
    }

    @Operation(summary = "Forgot password", description = "Changes password for the logged in user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email Successfully sent."),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
    })
    @PostMapping("forgot-password/")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody UserAuthenticationDto userDto) {
        return ResponseEntity
                .ok()
                .body(Map.of("message", "success"));
    }

    @PostMapping("{id}/change-password/")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody UserDto userDto, HttpServletRequest request) {
        userService.setPasswordForUser(userDto, id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("register/")
    public ResponseEntity<?> register(@RequestBody UserDto userDto, HttpServletRequest request) {
        if (request.getUserPrincipal() instanceof UserEntity) {
            UserEntity user = (UserEntity) request.getUserPrincipal();

        }
        userService.createUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
