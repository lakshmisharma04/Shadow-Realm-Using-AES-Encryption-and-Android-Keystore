package com.shadowrealm.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=test-only-jwt-secret-long-enough-for-hmac-sha256",
        "secret.encryption.key=test-only-secret-encryption-key-at-least-32"
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
