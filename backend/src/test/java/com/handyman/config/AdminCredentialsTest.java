package com.handyman.config;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class AdminCredentialsTest {
    @Test void prodFailsWithoutCredentialsEvenWithDevActive() {
        var env = new MockEnvironment();
        env.setActiveProfiles("dev", "prod");
        assertThrows(IllegalStateException.class, () -> new SecurityConfig().adminUsers(env));
    }
    @Test void prodFailsWithOnlyUsername() {
        var env = new MockEnvironment().withProperty("ADMIN_USERNAME", "admin");
        env.setActiveProfiles("prod");
        assertThrows(IllegalStateException.class, () -> new SecurityConfig().adminUsers(env));
    }
    @Test void prodRejectsPlaintextPassword() {
        var env = new MockEnvironment().withProperty("ADMIN_USERNAME", "admin").withProperty("ADMIN_PASSWORD_HASH", "plain");
        env.setActiveProfiles("prod");
        assertThrows(IllegalStateException.class, () -> new SecurityConfig().adminUsers(env));
    }
}
