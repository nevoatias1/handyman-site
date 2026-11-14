package com.handyman.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private static final int MAX_IPS = 10_000;
    private final AuthenticationManager manager;
    private final Clock clock;
    private final int capacity;
    private final Map<String, List<Instant>> failures = new HashMap<>();

    @Autowired
    public LoginService(AuthenticationManager manager) {
        this(manager, Clock.systemUTC(), MAX_IPS);
    }

    LoginService(AuthenticationManager manager, Clock clock, int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
        this.manager = manager;
        this.clock = clock;
        this.capacity = capacity;
    }

    // Serialize check + authentication + recording so concurrent requests cannot bypass the limit or cap.
    public synchronized Authentication login(String ip, String username, String password) {
        checkLimit(ip);
        try {
            Authentication authentication = manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password));
            failures.remove(ip);
            return authentication;
        } catch (AuthenticationException ex) {
            recordFailure(ip);
            throw ex;
        }
    }

    private void checkLimit(String ip) {
        Instant cutoff = clock.instant().minus(Duration.ofMinutes(15));
        failures.values().forEach(times -> times.removeIf(time -> !time.isAfter(cutoff)));
        failures.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (failures.getOrDefault(ip, List.of()).size() >= 5) throw new RateLimited();
        // Fail closed for untracked IPs at capacity; do not evict active limits to admit attackers.
        if (!failures.containsKey(ip) && failures.size() >= capacity) throw new RateLimited();
    }

    private void recordFailure(String ip) {
        failures.computeIfAbsent(ip, key -> new ArrayList<>()).add(clock.instant());
    }

    public synchronized boolean rejectInvalid(String ip) {
        try { checkLimit(ip); }
        catch (RateLimited ex) { return true; }
        recordFailure(ip);
        return false;
    }

    synchronized int trackedIpCount() { return failures.size(); }
    public static class RateLimited extends RuntimeException {}
}
