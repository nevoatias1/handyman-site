package com.handyman.config;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;

@Configuration
public class SecurityConfig {
    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    @Bean
    UserDetailsService adminUsers(Environment env) {
        boolean dev = !env.matchesProfiles("prod") && env.matchesProfiles("dev");
        String username = env.getProperty("ADMIN_USERNAME");
        String hash = env.getProperty("ADMIN_PASSWORD_HASH");
        if (dev) {
            if (username == null || username.isBlank()) username = "admin";
            if (hash == null || hash.isBlank()) hash = new BCryptPasswordEncoder(12).encode("dev-admin-change-me");
        }
        if (username == null || username.isBlank() || hash == null || !hash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
            throw new IllegalStateException("ADMIN_USERNAME and a valid BCrypt ADMIN_PASSWORD_HASH are required");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username).password(hash).roles("ADMIN").build());
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
        return new ProviderManager(provider);
    }

    @Bean
    CookieCsrfTokenRepository csrfRepository(Environment env) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Strict").secure(env.matchesProfiles("prod")).path("/"));
        return repository;
    }

    @Bean
    HttpSessionSecurityContextRepository securityContexts() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SessionAuthenticationStrategy loginStrategy(CookieCsrfTokenRepository csrf) {
        return new CompositeSessionAuthenticationStrategy(List.of(
            new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, CookieCsrfTokenRepository csrf,
            HttpSessionSecurityContextRepository contexts) throws Exception {
        http.cors(cors -> {})
            .csrf(config -> config.csrfTokenRepository(csrf)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .securityContext(config -> config.securityContextRepository(contexts))
            .requestCache(cache -> cache.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/logout", "/error").permitAll()
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, ex) -> error(response, 401, "נדרשת התחברות"))
                .accessDeniedHandler((request, response, ex) -> error(response, 403, "הבקשה אינה מורשית")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, auth) -> {
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":\"ok\"}");
                }));
        return http.build();
    }
}
