package com.shadowrealm.backend;

import com.shadowrealm.backend.service.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void tokenCarriesTheCurrentRevocationVersion() {
        JwtService service = new JwtService("unit-test-jwt-secret-with-at-least-32-bytes");
        String token = service.generateToken("player@example.com", 4);

        assertTrue(service.isTokenValid(token));
        assertEquals("player@example.com", service.extractEmail(token));
        assertEquals(4, service.extractTokenVersion(token));
    }

    @Test
    void rejectsWeakSigningSecretsAtStartup() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short"));
    }
}
