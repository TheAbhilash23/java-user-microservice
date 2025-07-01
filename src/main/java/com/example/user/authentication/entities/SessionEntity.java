package com.example.user.authentication.entities;

import com.example.user.users.entities.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "auth_sessions")
@Builder
@Entity
public class SessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Long sessionStartTime;

    private Long sessionEndTime;

    @ManyToOne
    private UserEntity user;

    @OneToMany(mappedBy = "session", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<TokenEntity> tokens = new ArrayList<>();

    public String toString(){
        return id + ": StartTime: " + sessionStartTime.toString();
    }
}
