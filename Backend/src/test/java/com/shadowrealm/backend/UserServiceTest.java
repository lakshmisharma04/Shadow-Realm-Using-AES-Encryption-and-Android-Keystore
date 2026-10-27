package com.shadowrealm.backend;

import com.shadowrealm.backend.dto.ChangePasswordRequest;
import com.shadowrealm.backend.dto.RegisterRequest;
import com.shadowrealm.backend.entity.User;
import com.shadowrealm.backend.exception.InvalidCredentialsException;
import com.shadowrealm.backend.repository.UserRepository;
import com.shadowrealm.backend.service.AuthenticationService;
import com.shadowrealm.backend.service.JwtService;
import com.shadowrealm.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository users;
    @Mock private BCryptPasswordEncoder passwords;
    @Mock private JwtService jwt;
    @Mock private AuthenticationService authentication;
    @InjectMocks private UserService service;

    @Test
    void registrationNormalizesEmailAndStoresOnlyThePasswordHash() {
        when(users.existsByEmail("player@example.com")).thenReturn(false);
        when(users.existsByUsername("Night Walker")).thenReturn(false);
        when(passwords.encode("password123")).thenReturn("$2a$test-hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequest request = new RegisterRequest();
        request.setUsername(" Night Walker ");
        request.setEmail(" PLAYER@Example.COM ");
        request.setPassword("password123");

        User saved = service.register(request);

        assertEquals("Night Walker", saved.getUsername());
        assertEquals("player@example.com", saved.getEmail());
        assertEquals("$2a$test-hash", saved.getPassword());
        verify(passwords).encode("password123");
    }

    @Test
    void passwordChangeChecksCurrentPasswordAndRevokesOlderTokens() {
        User user = new User();
        user.setEmail("player@example.com");
        user.setPassword("$2a$old-hash");
        when(authentication.getCurrentUser()).thenReturn(user);
        when(passwords.matches("old-password", "$2a$old-hash")).thenReturn(true);
        when(passwords.encode("new-password")).thenReturn("$2a$new-hash");
        when(jwt.generateToken("player@example.com", 1)).thenReturn("replacement-token");
        when(users.save(user)).thenReturn(user);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-password");
        request.setNewPassword("new-password");

        String replacementToken = service.changePassword(request);

        assertEquals("replacement-token", replacementToken);
        assertEquals("$2a$new-hash", user.getPassword());
        assertEquals(1, user.getTokenVersion());
        verify(jwt).generateToken("player@example.com", 1);
    }

    @Test
    void passwordChangeRejectsAnIncorrectCurrentPassword() {
        User user = new User();
        user.setPassword("$2a$old-hash");
        when(authentication.getCurrentUser()).thenReturn(user);
        when(passwords.matches("wrong-password", "$2a$old-hash")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong-password");
        request.setNewPassword("new-password");

        assertThrows(InvalidCredentialsException.class, () -> service.changePassword(request));
        verify(passwords, never()).encode(any());
    }
}
