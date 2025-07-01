package com.example.user.users.services;

import com.example.user.users.dto.UserDto;
import com.example.user.users.dto.UserPermissionsDto;
import com.example.user.users.entities.UserEntity;
import com.example.user.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private String getHashedPassword(String rawPassword) {
        return this.passwordEncoder.encode(rawPassword);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void createUser(UserDto userDto) throws ValidationException {
        boolean isUsernameTaken = userRepository.existsByUsername(userDto.getUsername());
        if (isUsernameTaken) {
            throw new ValidationException("Username already taken.");
        } else if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ValidationException("Email address already Registered.");
        }
        UserEntity user = UserEntity.builder()
                .email(userDto.getEmail())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .username(userDto.getUsername())
                .date(LocalDateTime.now())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .groups(userDto.getGroups())
                .build();
        userRepository.save(user);
    }

    public List<UserDto> getAll() {
        List<UserEntity> allUsers = userRepository.findAll();
        return allUsers.parallelStream().map(x -> UserDto.builder().
                id(x.getId()).
                username(x.getUsername()).
                firstName(x.getFirstName()).
                lastName(x.getLastName()).
                email(x.getEmail()).
                date(x.getDate()).
                password(x.getPassword()).
                build()).collect(Collectors.toList());
    }

    public UserDto getUserById(Long id) {

        UserEntity userEntity = userRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        return UserDto.builder().
                username(userEntity.getUsername()).
                firstName(userEntity.getFirstName()).
                lastName(userEntity.getLastName()).
                email(userEntity.getEmail()).
                id(userEntity.getId()).
                date(userEntity.getDate()).
                build();
    }

    public UserDto updateUserById(Long id, UserDto userData) {
        UserEntity old = userRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        // username and date can't change
        old.setUsername(userData.getUsername() != null && !userData.getUsername().isBlank() ? userData.getUsername() : old.getUsername());
        old.setDate(userData.getDate() != null && !userData.getDate().toString().isBlank() ? userData.getDate() : LocalDateTime.now());

        // first name, last name can change
        old.setFirstName(userData.getFirstName() != null && !userData.getFirstName().isBlank() ? userData.getFirstName() : old.getFirstName());
        old.setLastName(userData.getLastName() != null && !userData.getLastName().isBlank() ? userData.getLastName() : old.getLastName());
        userRepository.save(old);
        return UserDto.builder().
                username(old.getUsername()).
                email(old.getEmail()).
                id(old.getId()).
                date(old.getDate()).
                build();
    }

    public boolean deleteUserById(Long id) {
        boolean existsById = userRepository.existsById(id);
        if (existsById) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }



    /**
     * Sets a new password for the user with the given id, using the password from the provided UserDto.
     *
     * @param userDto UserDto containing the new password (in plain text)
     * @param id      The id of the user whose password is to be changed
     * @return true if password was updated, false if user not found
     */
    public boolean setPasswordForUser(UserDto userDto, Long id) {
        UserEntity user = userRepository.findById(id).orElseThrow();
        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw new ValidationException("Password cannot be empty");
        }
        System.out.println(passwordEncoder.encode(userDto.getPassword()));
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        userRepository.save(user);
        return true;
    }

    public List<UserPermissionsDto> getAllUserPermissionsDto(Long id) {
        return userRepository.getUserPermissions(id);
    }

    public Map<String, Set<String>> getUserModulePermissionsMap(Long id) {
        List<UserPermissionsDto> userPermissionsDtoList = getAllUserPermissionsDto(id);
        return userPermissionsDtoList
                .stream()
                .collect(Collectors.toMap(
                        UserPermissionsDto::getModuleName,
                        x -> new HashSet<>(Set.of(x.getPermissionName() != null ? x.getPermissionName() : "")),
                        (old, newMerge) -> old.addAll(newMerge) ? old : old,
                        TreeMap::new
                        )
                );

    }

    public Set<String> getUserRoles(Long id){
        List<UserPermissionsDto> userPermissions = userRepository.getUserPermissions(id);
        return userPermissions
                .stream()
                .map(x->x.getGroupName() + "," + x.getPermissionName() + "," + x.getModuleName())
                .collect(Collectors.toSet());
    }
}
