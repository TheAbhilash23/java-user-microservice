package com.example.user.core.security;


import com.example.user.users.services.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs/swagger-config",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "authentication/**"
                        ).permitAll()
                        .anyRequest()
                        .permitAll()
                )
                .logout(logout -> logout.logoutSuccessUrl("/login"))
                .csrf(csrf->csrf.disable());

        return http.build();
    }

    @Bean
    protected AuthenticationManager authManager(HttpSecurity http, PasswordEncoder encoder,
                                             UserService userDetailsService) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Pbkdf2PasswordEncoder(
            "",
            32,
            256,
            Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256
        );
    }

}
