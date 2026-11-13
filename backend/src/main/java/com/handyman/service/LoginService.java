package com.handyman.service;

import java.time.*;
import java.util.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private final AuthenticationManager manager;
    private final Map<String, List<Instant>> failures = new HashMap<>();
    public LoginService(AuthenticationManager manager) { this.manager = manager; }

    // Serialize check + authentication + failure recording so concurrent requests cannot bypass the limit.
    public synchronized Authentication login(String ip, String username, String password) {
        checkLimit(ip);
        try {
            return manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        } catch (AuthenticationException ex) {
            failures.computeIfAbsent(ip, key -> new ArrayList<>()).add(Instant.now());
            throw ex;
        }
    }
    private void checkLimit(String ip) {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(15));
        failures.values().forEach(times -> times.removeIf(time -> !time.isAfter(cutoff)));
        failures.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (failures.getOrDefault(ip, List.of()).size() >= 5) throw new RateLimited();
    }
    public synchronized boolean rejectInvalid(String ip) {
        try { checkLimit(ip); }
        catch (RateLimited ex) { return true; }
        failures.computeIfAbsent(ip, key -> new ArrayList<>()).add(Instant.now());
        return false;
    }
    public static class RateLimited extends RuntimeException {}
}
