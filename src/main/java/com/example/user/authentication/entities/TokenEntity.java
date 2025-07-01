package com.example.user.authentication.entities;

import jakarta.persistence.*;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "auth_tokens")
@Builder
@Entity
public class TokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private Long iat;

    private Long exp;

    private String token;

    private Boolean isPrematureTerminated;

    @ManyToOne
    private SessionEntity session;

    @Override
    public String toString() {
        return this.token + this.exp.toString();
    }

}