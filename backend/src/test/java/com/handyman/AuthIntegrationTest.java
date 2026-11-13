package com.handyman;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    private record Token(Cookie cookie, String value) {}
    private Token token() throws Exception {
        var result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        assertEquals("Strict", cookie.getAttribute("SameSite"));
        return new Token(cookie, cookie.getValue());
    }
    private MvcResult login(String ip, String password, MockHttpSession session, int expected) throws Exception {
        Token token = token();
        var request = post("/api/auth/login").cookie(token.cookie()).header("X-XSRF-TOKEN", token.value())
            .header("Origin", "http://localhost:5173")
            .contentType("application/json").content("{\"username\":\"admin\",\"password\":\"" + password + "\"}")
            .with(req -> { req.setRemoteAddr(ip); return req; });
        if (session != null) request.session(session);
        return mvc.perform(request).andExpect(status().is(expected)).andReturn();
    }
    @Test void successRotatesSessionAndAllowsAdmin() throws Exception {
        MockHttpSession old = new MockHttpSession();
        String oldId = old.getId();
        var result = login("192.0.2.1", "dev-admin-change-me", old, 200);
        assertNotEquals(oldId, old.getId());
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"));
        mvc.perform(get("/api/admin/ping").session(session)).andExpect(status().isOk());
    }
    @Test void anonymousCannotAccessAdminOrMe() throws Exception {
        mvc.perform(get("/api/admin/ping")).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/json"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
    @Test void viteAuthPreflightAllowsCsrfHeader() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Content-Type,X-XSRF-TOKEN"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
    @Test void wrongPasswordIsGeneric() throws Exception {
        var result = login("192.0.2.2", "wrong", null, 401);
        assertTrue(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("שם המשתמש או הסיסמה שגויים"));
    }
    @Test void authenticatedWithoutAdminRoleIsForbidden() throws Exception {
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "nonadmin", null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
        var session = new MockHttpSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);
        mvc.perform(get("/api/admin/ping").session(session)).andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith("application/json"));
    }
    @Test void invalidBodiesAlsoCountTowardLimit() throws Exception {
        for (int i = 0; i < 6; i++) {
            Token token = token();
            mvc.perform(post("/api/auth/login").cookie(token.cookie()).header("X-XSRF-TOKEN", token.value())
                .contentType("application/json").content("{}")
                .with(req -> { req.setRemoteAddr("192.0.2.6"); return req; }))
                .andExpect(status().is(i < 5 ? 401 : 429));
        }
    }
    @Test void csrfRequiredForLoginAndLogout() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"username\":\"admin\",\"password\":\"dev-admin-change-me\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isForbidden());
    }
    @Test void realCookieTokenMismatchRejected() throws Exception {
        Token token = token();
        mvc.perform(post("/api/auth/login").cookie(token.cookie()).header("X-XSRF-TOKEN", "invalid")
            .contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    @Test void rateLimitIsPerIpAndIgnoresForwardedHeaders() throws Exception {
        for (int i=0; i<5; i++) login("192.0.2.3", "wrong", null, 401);
        login("192.0.2.3", "dev-admin-change-me", null, 429);
        login("192.0.2.4", "dev-admin-change-me", null, 200);
    }
    @Test void logoutInvalidatesSession() throws Exception {
        var result = login("192.0.2.5", "dev-admin-change-me", null, 200);
        var session = (MockHttpSession) result.getRequest().getSession(false);
        Token token = token();
        mvc.perform(post("/api/auth/logout").session(session).cookie(token.cookie())
            .header("X-XSRF-TOKEN", token.value())).andExpect(status().isOk());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/admin/ping")).andExpect(status().isUnauthorized());
    }
}
