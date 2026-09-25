package com.workspace.api.user;

import com.workspace.api.user.repository.RefreshTokenRepository;
import com.workspace.api.user.repository.UserRepository;
import com.workspace.api.user.security.RefreshCookie;
import com.workspace.api.user.service.RefreshSessionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// No test transaction: replay revocations must remain committed after the HTTP error.
@SpringBootTest(properties = {"app.jwt.ttl=PT15M", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshSessionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired RefreshTokenRepository tokens;
    @Autowired UserRepository users;

    private record Login(String email, String userId, String access, Cookie cookie) {}

    @Test
    void loginUsesHttpOnlyCookieAndStoresOnlyHash() throws Exception {
        Login account = register();
        var result = login(account.email());
        Cookie cookie = result.getCookie(RefreshCookie.NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(result.getHeader("Set-Cookie")).contains("SameSite=Strict");
        assertThat(result.getHeader("Cache-Control")).contains("no-store");
        assertThat(result.getContentAsString()).doesNotContain(cookie.getValue()).doesNotContain("refreshToken");
        assertThat(json.readTree(result.getContentAsString()).path("expiresIn").asLong()).isEqualTo(900);
        var stored = tokens.findByTokenHash(RefreshSessionService.hash(cookie.getValue())).orElseThrow();
        assertThat(stored.getTokenHash()).hasSize(64).isNotEqualTo(cookie.getValue());
        mvc.perform(post("/users/login").header("X-PlannerFy-Session", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", account.email(), "password", "wrong"))))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void refreshRotatesAndKeepsAbsoluteDeadline() throws Exception {
        Login account = register();
        var old = tokens.findByTokenHash(RefreshSessionService.hash(account.cookie().getValue())).orElseThrow();
        var response = refresh(account.cookie(), 200);
        var next = response.getCookie(RefreshCookie.NAME);
        assertThat(next.getValue()).isNotEqualTo(account.cookie().getValue());
        var updated = tokens.findByTokenHash(old.getTokenHash()).orElseThrow();
        var replacement = tokens.findByTokenHash(RefreshSessionService.hash(next.getValue())).orElseThrow();
        assertThat(updated.getUsedAt()).isNotNull();
        assertThat(replacement.getFamilyId()).isEqualTo(old.getFamilyId());
        assertThat(replacement.getAbsoluteExpiresAt()).isEqualTo(old.getAbsoluteExpiresAt());
        String access = json.readTree(response.getContentAsString()).path("accessToken").asString();
        assertThat(access).isNotEqualTo(account.access());
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(account.userId()));
    }

    @Test
    void expiredIdleAndAbsoluteDeadlinesAreRejected() throws Exception {
        for (boolean absolute : new boolean[]{false, true}) {
            Login account = register();
            var token = tokens.findByTokenHash(RefreshSessionService.hash(account.cookie().getValue())).orElseThrow();
            token.setCreatedAt(Instant.now().minusSeconds(100L * 86400));
            token.setExpiresAt(Instant.now().minusSeconds(2));
            if (absolute) token.setAbsoluteExpiresAt(Instant.now().minusSeconds(1));
            tokens.saveAndFlush(token);
            refresh(account.cookie(), 401);
            assertThat(tokens.findById(token.getId()).orElseThrow().getRevokedAt()).isNotNull();
        }
    }

    @Test
    void replayRevokesDescendantsButNotOtherDeviceOrOtherUser() throws Exception {
        Login account = register();
        Cookie otherDevice = login(account.email()).getCookie(RefreshCookie.NAME);
        Login otherUser = register();
        Cookie second = refresh(account.cookie(), 200).getCookie(RefreshCookie.NAME);
        Cookie third = refresh(second, 200).getCookie(RefreshCookie.NAME);
        refresh(account.cookie(), 401);
        refresh(third, 401);
        refresh(otherDevice, 200);
        refresh(otherUser.cookie(), 200);
    }

    @Test
    void logoutRevokesOnlyCurrentFamilyAndClearsCookie() throws Exception {
        Login account = register();
        Cookie other = login(account.email()).getCookie(RefreshCookie.NAME);
        var response = mvc.perform(post("/users/logout").header("X-PlannerFy-Session", "1").cookie(account.cookie()))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        assertThat(response.getCookie(RefreshCookie.NAME).getMaxAge()).isZero();
        refresh(account.cookie(), 401);
        refresh(other, 200);
        mvc.perform(post("/users/logout").header("X-PlannerFy-Session", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutAllRevokesEveryDeviceOfAuthenticatedAccount() throws Exception {
        Login account = register();
        Cookie otherDevice = login(account.email()).getCookie(RefreshCookie.NAME);
        Login otherUser = register();
        mvc.perform(post("/users/logout-all").header("X-PlannerFy-Session", "1")
                        .header("Authorization", "Bearer " + account.access()))
                .andExpect(status().isNoContent());
        refresh(account.cookie(), 401);
        refresh(otherDevice, 401);
        refresh(otherUser.cookie(), 200);
    }

    @Test
    void disabledUserCannotRefresh() throws Exception {
        Login account = register();
        var user = users.findById(account.userId()).orElseThrow();
        user.setEnabled(false);
        users.saveAndFlush(user);
        refresh(account.cookie(), 401);
    }

    @Test
    void csrfAndCorsProtectCookieEndpoints() throws Exception {
        for (String path : new String[]{"login", "register", "refresh", "logout", "logout-all"}) {
            mvc.perform(post("/users/" + path)).andExpect(status().isForbidden());
            mvc.perform(post("/users/" + path).header("X-PlannerFy-Session", "1")
                    .header("Origin", "https://attacker.example")).andExpect(status().isForbidden());
        }
        mvc.perform(options("/users/refresh").header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-PlannerFy-Session"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(post("/users/refresh").header("X-PlannerFy-Session", "1"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("REFRESH_UNAVAILABLE"));
        refresh(new Cookie(RefreshCookie.NAME, "invalid"), 401);
    }

    @Test
    void concurrentReuseCannotCreateTwoActiveBranches() throws Exception {
        Login account = register();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<MockHttpServletResponse> action = () -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return mvc.perform(post("/users/refresh").header("X-PlannerFy-Session", "1")
                        .cookie(account.cookie())).andReturn().getResponse();
            };
            var first = executor.submit(action);
            var second = executor.submit(action);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var a = first.get(20, TimeUnit.SECONDS);
            var b = second.get(20, TimeUnit.SECONDS);
            assertThat(java.util.List.of(a.getStatus(), b.getStatus())).containsExactlyInAnyOrder(200, 401);
            refresh((a.getStatus() == 200 ? a : b).getCookie(RefreshCookie.NAME), 401);
        }
    }

    private Login register() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        var response = mvc.perform(post("/users/register").header("X-PlannerFy-Session", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                                Map.of("name", "Tester", "email", email, "password", "test-password"))))
                .andExpect(status().isCreated()).andReturn().getResponse();
        var body = json.readTree(response.getContentAsString());
        return new Login(email, body.path("user").path("id").asString(),
                body.path("accessToken").asString(), response.getCookie(RefreshCookie.NAME));
    }

    private MockHttpServletResponse login(String email) throws Exception {
        return mvc.perform(post("/users/login").header("X-PlannerFy-Session", "1")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(
                                Map.of("email", email, "password", "test-password"))))
                .andExpect(status().isOk()).andReturn().getResponse();
    }

    private MockHttpServletResponse refresh(Cookie cookie, int expected) throws Exception {
        return mvc.perform(post("/users/refresh").header("X-PlannerFy-Session", "1").cookie(cookie))
                .andExpect(status().is(expected)).andReturn().getResponse();
    }
}
