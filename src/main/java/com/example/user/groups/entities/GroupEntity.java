package com.example.user.groups.entities;

import com.example.user.permissions.entities.ModuleGroupEntity;
import com.example.user.users.entities.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Group entity representing a user's role(s) the system")
@Data
@Builder
@Table(name = "groups")
@Entity
public class GroupEntity {

    @Schema(description = "Unique identifier of the group", example = "123456789")
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_seq")
    @SequenceGenerator(name = "group_seq", sequenceName = "group_seq", allocationSize = 1)
    private Long id;

    @Schema(description = "Group name", example = "s")
    @Column(nullable = false, length = 10)
    private String name;

    @Schema(description = "Users to which this group is mapped to", example = "1")
    @ManyToMany(mappedBy = "groups", fetch = FetchType.LAZY)
    private List<UserEntity> users = new ArrayList<>();

    @Schema(description = "List of modules mapped to this group")
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<ModuleGroupEntity> moduleGroups = new ArrayList<>();
}

