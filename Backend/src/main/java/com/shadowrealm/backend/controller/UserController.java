package com.shadowrealm.backend.controller;

import com.shadowrealm.backend.dto.LoginRequest;
import com.shadowrealm.backend.dto.LoginResponse;
import com.shadowrealm.backend.dto.ChangePasswordRequest;
import com.shadowrealm.backend.dto.RegisterRequest;
import com.shadowrealm.backend.dto.UserResponse;
import com.shadowrealm.backend.entity.User;
import com.shadowrealm.backend.service.UserService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {

        User saved = userService.register(request);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {

        String token = userService.login(request);

        return new LoginResponse(token);
    }

    @PutMapping("/password")
    public ResponseEntity<LoginResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String token = userService.changePassword(request);
        return ResponseEntity.ok(new LoginResponse(token));
    }
}