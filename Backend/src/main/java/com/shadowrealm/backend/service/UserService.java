package com.shadowrealm.backend.service;

import com.shadowrealm.backend.dto.LoginRequest;
import com.shadowrealm.backend.dto.RegisterRequest;
import com.shadowrealm.backend.dto.ChangePasswordRequest;
import com.shadowrealm.backend.entity.User;
import com.shadowrealm.backend.exception.DuplicateResourceException;
import com.shadowrealm.backend.exception.InvalidCredentialsException;
import com.shadowrealm.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationService authenticationService;

    @Transactional
    public User register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = request.getUsername().trim();
        ensureBcryptPasswordLength(request.getPassword());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists");
        }

        // Construct a fresh entity so callers cannot set an id or any future
        // server-managed fields through JSON mass assignment.
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        return userRepository.save(user);
    }

    @Transactional
    public String changePassword(ChangePasswordRequest request) {
        User user = authenticationService.getCurrentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        ensureBcryptPasswordLength(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        return jwtService.generateToken(user.getEmail(), user.getTokenVersion());
    }

    public String login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new InvalidCredentialsException("Invalid email or password");
        }

        return jwtService.generateToken(user.getEmail(), user.getTokenVersion());
    }

    private void ensureBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Password must not exceed 72 UTF-8 bytes.");
        }
    }
}