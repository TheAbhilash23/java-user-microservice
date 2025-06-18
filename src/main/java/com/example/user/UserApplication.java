package com.example.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication(exclude = {net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration.class})
@EnableJpaAuditing
public class UserApplication {

    public static void main(String[] args) {

        SpringApplication app = new SpringApplication(UserApplication.class);

        // Hardcode the external config location
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("spring.config.location", "file:/home/abhilash/Desktop/java-microservices/user-microservice/src/main/resources/application.properties");
        app.setDefaultProperties(defaultProps);
        app.run(args);
    }
}
