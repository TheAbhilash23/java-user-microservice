package com.example.user.users.controllers;

import com.example.user.core.controllers.BaseController;
import com.example.user.users.dto.UserDto;
import com.example.user.users.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Convention is : controller --> service --> repository

@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController extends BaseController<UserDto> {

    @Autowired
    private UserService userService;

    @Override
    @GetMapping("")
    public ResponseEntity<List<UserDto>> list() {
        return new ResponseEntity<>(userService.getAll(), HttpStatus.OK);
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> retrieve(@PathVariable Long id) {
        UserDto user = userService.getUserById(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @Override
    @PostMapping("/")
    public ResponseEntity<?> create(@RequestBody UserDto newUser) {
        newUser.setId(null);
        if (newUser.getPassword() != null && newUser.getPassword().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error", "Password empty",
                            "message", "Please Enter Password")
                    );
        }
        try {
            userService.createUser(newUser);
            return new ResponseEntity<>(true, HttpStatus.OK);
        } catch (ValidationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @PatchMapping("/{id}/")
    public ResponseEntity<UserDto> partialUpdate(@PathVariable Long id, @RequestBody UserDto updateUser) {
        UserDto user = userService.updateUserById(id, updateUser);
        return ResponseEntity
                .ok()
                .body(user);
    }


    @Override
    @DeleteMapping("/{id}/")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        boolean isDeleted = userService.deleteUserById(id);
        if (isDeleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("{id}/user-permissions")
    public ResponseEntity<?> userPermissions(@PathVariable Long id) {
        return ResponseEntity.ofNullable(userService.getUserModulePermissionsMap(id));
    }
}
