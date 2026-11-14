package com.handyman.config;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

class AdminCredentialsTest {
    private MockEnvironment environment(Map<String,Object> variables, String... profiles) {
        var env = new MockEnvironment() {
            @Override public Map<String,Object> getSystemEnvironment() { return variables; }
        };
        env.setActiveProfiles(profiles);
        return env;
    }
    @Test void devHasNoDefaults() {
        assertThrows(IllegalStateException.class, () -> new SecurityConfig().adminUsers(environment(Map.of(), "dev")));
    }
    @Test void devUsesLocalHashAndEnvironmentPropertiesOverrideIt() {
        String password = java.util.UUID.randomUUID().toString();
        String hash = new BCryptPasswordEncoder(12).encode(password);
        var env = environment(Map.of(), "dev");
        env.setProperty("local.admin.username", "local-user");
        env.setProperty("local.admin.password-hash", hash);
        var users = new SecurityConfig().adminUsers(env);
        assertTrue(new BCryptPasswordEncoder().matches(password, users.loadUserByUsername("local-user").getPassword()));
        env.setProperty("ADMIN_USERNAME", "override-user");
        env.setProperty("ADMIN_PASSWORD_HASH", hash);
        assertNotNull(new SecurityConfig().adminUsers(env).loadUserByUsername("override-user"));
    }
    @Test void prodIgnoresLocalFileEvenAlongsideDev() {
        var env = environment(Map.of(), "dev", "prod");
        env.setProperty("local.admin.username", "local-user");
        env.setProperty("local.admin.password-hash", new BCryptPasswordEncoder().encode("test-only"));
        assertThrows(IllegalStateException.class, () -> new SecurityConfig().adminUsers(env));
    }
    @Test void prodFailsWithOnlyUsername() {
        assertThrows(IllegalStateException.class, () -> new SecurityConfig().adminUsers(environment(Map.of("ADMIN_USERNAME", "admin"), "prod")));
    }
    @Test void prodRejectsPlaintextPassword() {
        assertThrows(IllegalStateException.class, () -> new SecurityConfig().adminUsers(environment(Map.of("ADMIN_USERNAME", "admin", "ADMIN_PASSWORD_HASH", "plain"), "prod")));
    }
}