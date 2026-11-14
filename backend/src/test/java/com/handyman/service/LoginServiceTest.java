package com.handyman.service;

import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import static org.junit.jupiter.api.Assertions.*;

class LoginServiceTest {
    private static final AuthenticationManager MANAGER = request -> {
        if (!"correct".equals(request.getCredentials())) throw new BadCredentialsException("Invalid");
        return UsernamePasswordAuthenticationToken.authenticated("admin", null, java.util.List.of());
    };
    private static class TestClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
    private void fail(LoginService service, String ip) {
        assertThrows(BadCredentialsException.class, () -> service.login(ip, "admin", "wrong"));
    }
    @Test void successClearsFailuresForOnlyThatIp() {
        var service = new LoginService(MANAGER, new TestClock(), 10);
        for (int i=0; i<4; i++) { fail(service, "a"); fail(service, "b"); }
        Authentication result = service.login("a", "admin", "correct");
        assertEquals("admin", result.getName());
        for (int i=0; i<5; i++) fail(service, "a");
        assertThrows(LoginService.RateLimited.class, () -> service.login("a", "admin", "correct"));
        fail(service, "b");
        assertThrows(LoginService.RateLimited.class, () -> service.login("b", "admin", "correct"));
    }
    @Test void expiredEntriesAreEvictedAndCapacityCanBeReused() {
        var clock = new TestClock();
        var service = new LoginService(MANAGER, clock, 2);
        fail(service, "a");
        clock.now = clock.now.plusSeconds(60);
        fail(service, "b");
        clock.now = clock.now.plusSeconds(14 * 60);
        // Exactly 15 minutes: a expires, while b remains active.
        fail(service, "c");
        assertEquals(2, service.trackedIpCount());
        assertThrows(LoginService.RateLimited.class, () -> service.login("d", "admin", "wrong"));
        clock.now = clock.now.plusSeconds(15 * 60);
        service.login("d", "admin", "correct");
        assertEquals(0, service.trackedIpCount());
    }
    @Test void capacityRejectsNewIpsWithoutDiscardingActiveLimits() {
        var service = new LoginService(MANAGER, new TestClock(), 2);
        for (int i=0; i<5; i++) fail(service, "a");
        fail(service, "b");
        for (int i=0; i<100; i++) {
            final String ip = "new-" + i;
            assertThrows(LoginService.RateLimited.class, () -> service.login(ip, "admin", "wrong"));
            assertTrue(service.rejectInvalid(ip));
        }
        assertEquals(2, service.trackedIpCount());
        assertThrows(LoginService.RateLimited.class, () -> service.login("a", "admin", "correct"));
        service.login("b", "admin", "correct");
        assertEquals(1, service.trackedIpCount());
        fail(service, "c");
        assertEquals(2, service.trackedIpCount());
    }
}
