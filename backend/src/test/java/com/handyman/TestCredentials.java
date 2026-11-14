package com.handyman;

import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

abstract class TestCredentials {
    static final String TEST_PASSWORD = UUID.randomUUID().toString();
    private static final String TEST_HASH = new BCryptPasswordEncoder(12).encode(TEST_PASSWORD);
    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("ADMIN_USERNAME", () -> "test-admin");
        registry.add("ADMIN_PASSWORD_HASH", () -> TEST_HASH);
    }
}