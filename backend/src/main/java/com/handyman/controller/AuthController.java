package com.handyman.controller;

import java.io.IOException;
import java.util.Map;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import com.handyman.config.SecurityConfig;
import com.handyman.dto.LoginRequest;
import com.handyman.service.LoginService;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final LoginService logins;
    private final SessionAuthenticationStrategy strategy;
    private final HttpSessionSecurityContextRepository contexts;
    public AuthController(LoginService logins, SessionAuthenticationStrategy strategy, HttpSessionSecurityContextRepository contexts) {
        this.logins = logins; this.strategy = strategy; this.contexts = contexts;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = logins.login(request.getRemoteAddr(), body.username(), body.password());
        strategy.onAuthentication(auth, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return Map.of("username", auth.getName());
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication auth) { return Map.of("username", auth.getName()); }

    @ExceptionHandler(LoginService.RateLimited.class)
    void limited(HttpServletResponse response) throws IOException {
        response.setHeader("Retry-After", "900");
        SecurityConfig.error(response, 429, "יותר מדי ניסיונות התחברות. נסו שוב מאוחר יותר");
    }

    @ExceptionHandler(AuthenticationException.class)
    void failed(HttpServletResponse response) throws IOException {
        SecurityConfig.error(response, 401, "שם המשתמש או הסיסמה שגויים");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    void invalid(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (logins.rejectInvalid(request.getRemoteAddr())) limited(response);
        else failed(response);
    }
}
