package com.workspace.api.user;

import com.workspace.api.user.domain.User;
import com.workspace.api.user.repository.UserRepository;
import com.workspace.api.util.domain.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MultiUserSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;

    private record Account(String id, String token) {}
    private static final String PASSWORD = "a-test-password";
    private static final String GOAL = """
            {"name":"Study","expectedTarget":"Five lessons","priority":"NORMAL","category":"STUDY","weekId":"2026-W38"}
            """;

    @ParameterizedTest
    @ValueSource(strings = {"/users/me", "/notes", "/goals", "/goals/board", "/events", "/planner/events",
            "/planner/availability?date=2026-09-15", "/event", "/habit", "/habit/planner?date=2026-09-15",
            "/schedule/tasks/getTask", "/schedule/tasks/taskByDate", "/events/getEventCount",
            "/event/getEventCount", "/weather?latitude=0&longitude=0"})
    void requiresAuthenticationAcrossFeatures(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @Test
    void registersLogsInHashesPasswordsAndPreventsPrivilegeInjection() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        JsonNode registered = body(mvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("name", "Alice", "email", email,
                                "password", PASSWORD, "role", "ADMIN", "userId", "forged"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist()).andReturn().getResponse().getContentAsString());
        User stored = users.findById(registered.path("user").path("id").asText()).orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, stored.getPasswordHash())).isTrue();

        String token = registered.path("accessToken").asText();
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(stored.getId()));
        mvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"));
        mvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", email, "password", "wrong"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("name", "Again", "email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidRegistrationAndOversizedUtf8Passwords() throws Exception {
        for (String payload : new String[] {
                "{\"name\":\"\",\"email\":\"invalid\",\"password\":\"short\"}",
                json.writeValueAsString(java.util.Map.of("name", "Alice", "email", "valid@example.com",
                        "password", "é".repeat(40)))}) {
            mvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void rejectsInvalidExpiredWrongIssuerAndDisabledAccountTokens() throws Exception {
        Account account = account();
        String[] parts = account.token().split("\\.");
        char first = parts[2].charAt(0);
        String tampered = parts[0] + "." + parts[1] + "." + (first == 'A' ? "B" : "A") + parts[2].substring(1);
        for (String token : new String[]{"malformed", tampered,
                signed(account.id(), "workspace-api-test", Instant.now().minusSeconds(120)),
                signed(account.id(), "wrong-issuer", Instant.now().plusSeconds(600)),
                signed(UUID.randomUUID().toString(), "workspace-api-test", Instant.now().plusSeconds(600))}) {
            mvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
        User user = users.findById(account.id()).orElseThrow();
        user.setEnabled(false);
        users.saveAndFlush(user);
        mvc.perform(auth(get("/notes"), account)).andExpect(status().isUnauthorized());
        mvc.perform(post("/users/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(java.util.Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyRoleOnlySeesAndChangesItsOwnData(Role role) throws Exception {
        Account alice = account();
        Account bob = account();
        User second = users.findById(bob.id()).orElseThrow();
        second.setRole(role);
        users.saveAndFlush(second);

        JsonNode note = create("/notes", "{\"title\":\"Private\",\"userId\":\"" + bob.id() + "\"}", alice, 201);
        JsonNode task = create("/schedule/tasks", "{\"title\":\"Private\",\"userId\":\"" + bob.id() + "\"}", alice, 200);
        JsonNode legacyEvent = create("/event", "{\"name\":\"Private\"}", alice, 201);
        JsonNode event = create("/events", eventJson(), alice, 201);
        JsonNode habit = create("/habit", "{\"name\":\"Private\"}", alice, 201);
        JsonNode goal = create("/goals", GOAL, alice, 201);
        for (JsonNode value : new JsonNode[]{note, task, legacyEvent, event, habit, goal}) {
            assertThat(value.path("userId").asText()).isEqualTo(alice.id());
        }

        for (String path : new String[]{"/notes", "/schedule/tasks/getTask", "/event", "/events", "/planner/events",
                "/habit", "/habit/planner?date=2026-09-15", "/goals"}) {
            mvc.perform(auth(get(path).param("userId", alice.id()), bob))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));
        }
        for (String path : new String[]{"/schedule/tasks/taskByDate", "/event/getEventCount", "/events/getEventCount"}) {
            mvc.perform(auth(get(path), bob)).andExpect(status().isOk()).andExpect(content().string("0"));
            mvc.perform(auth(get(path), alice)).andExpect(status().isOk()).andExpect(content().string("1"));
        }
        mvc.perform(auth(get("/goals/board").param("weekId", "2026-W38"), bob))
                .andExpect(jsonPath("$.summary.total").value(0));
        mvc.perform(auth(get("/planner/availability").param("date", LocalDate.now().toString()), bob))
                .andExpect(jsonPath("$.availableBlocks").value(12));

        mvc.perform(auth(delete("/notes/" + note.path("id").asText()), bob)).andExpect(status().isNotFound());
        mvc.perform(auth(put("/schedule/tasks/" + task.path("id").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Stolen\"}"), bob))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete("/schedule/tasks/" + task.path("id").asText()), bob)).andExpect(status().isNotFound());
        for (String prefix : new String[]{"/events/", "/planner/events/"}) {
            mvc.perform(auth(put(prefix + event.path("id").asText()).contentType(MediaType.APPLICATION_JSON)
                    .content(eventJson()), bob)).andExpect(status().isNotFound());
            mvc.perform(auth(delete(prefix + event.path("id").asText()), bob)).andExpect(status().isNotFound());
        }
        mvc.perform(auth(post("/habit/" + habit.path("id").asText() + "/completions/2026-09-15/toggle"), bob))
                .andExpect(status().isNotFound());
        String goalPath = "/goals/" + goal.path("id").asText();
        mvc.perform(auth(get(goalPath), bob)).andExpect(status().isNotFound());
        mvc.perform(auth(put(goalPath).contentType(MediaType.APPLICATION_JSON).content(GOAL), bob))
                .andExpect(status().isNotFound());
        mvc.perform(auth(delete(goalPath), bob)).andExpect(status().isNotFound());
        mvc.perform(auth(post(goalPath + "/complete"), bob)).andExpect(status().isNotFound());
        mvc.perform(auth(post(goalPath + "/defer"), bob)).andExpect(status().isNotFound());

        // Reusing another user's id in the legacy creation endpoint must create a new record.
        JsonNode ownLegacy = create("/event", "{\"id\":" + legacyEvent.path("id").asInt() +
                ",\"name\":\"Own\",\"userId\":\"" + alice.id() + "\"}", bob, 201);
        assertThat(ownLegacy.path("id").asInt()).isNotEqualTo(legacyEvent.path("id").asInt());
        assertThat(ownLegacy.path("userId").asText()).isEqualTo(bob.id());
        mvc.perform(auth(get("/event"), alice)).andExpect(jsonPath("$[0].name").value("Private"));
        mvc.perform(auth(get("/schedule/tasks/getTask"), alice)).andExpect(jsonPath("$[0].title").value("Private"));
        mvc.perform(auth(get("/goals/" + goal.path("id").asText()), alice)).andExpect(status().isOk());

        // Both users can book the same hour, but one user cannot book it twice.
        create("/planner/events", eventJson(), bob, 201);
        create("/planner/events", eventJson(), bob, 409);
        mvc.perform(auth(put("/schedule/tasks/" + task.path("id").asText()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true,\"userId\":\"" + bob.id() + "\"}"), alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(alice.id()));
        mvc.perform(auth(delete("/schedule/tasks/" + task.path("id").asText()), alice)).andExpect(status().isNoContent());
        mvc.perform(auth(delete("/notes/" + note.path("id").asText()), alice)).andExpect(status().isNoContent());
        mvc.perform(auth(post("/habit/" + habit.path("id").asText() + "/completions/2026-09-15/toggle"), alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completedDates[0]").value("2026-09-15"));
    }

    @Test
    void weeklyFocusBelongsToBothUserAndWeek() throws Exception {
        Account alice = account();
        Account bob = account();
        mvc.perform(auth(put("/goals/focus/2026-W38").contentType(MediaType.APPLICATION_JSON)
                .content("{\"focus\":\"Alice\"}"), alice)).andExpect(status().isOk());
        mvc.perform(auth(get("/goals/board").param("weekId", "2026-W38"), bob))
                .andExpect(jsonPath("$.focus").value(nullValue()));
        mvc.perform(auth(put("/goals/focus/2026-W38").contentType(MediaType.APPLICATION_JSON)
                .content("{\"focus\":\"Bob\"}"), bob)).andExpect(status().isOk());
        mvc.perform(auth(get("/goals/board").param("weekId", "2026-W38"), alice))
                .andExpect(jsonPath("$.focus.focus").value("Alice"));
        mvc.perform(auth(get("/goals/board").param("weekId", "2026-W38"), bob))
                .andExpect(jsonPath("$.focus.focus").value("Bob"));
    }

    @Test
    void databaseRoleChangesApplyToExistingTokens() throws Exception {
        Account member = account();
        User user = users.findById(member.id()).orElseThrow();
        for (Role role : new Role[]{Role.USER_PLUS, Role.ADMIN, Role.USER}) {
            user.setRole(role);
            users.saveAndFlush(user);
            mvc.perform(auth(get("/users/me"), member))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(role.name()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users/register", "/users/login", "/users/123/role", "/notes", "/habit", "/event",
            "/events", "/planner/events", "/goals", "/schedule/tasks"})
    void corsPreflightAllowsAuthorizationAcrossFeatures(String path) throws Exception {
        mvc.perform(options(path).header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", path.endsWith("/role") ? "PATCH" : "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    private Account account() throws Exception {
        JsonNode response = create("/users/register", json.writeValueAsString(java.util.Map.of(
                "name", "Tester", "email", UUID.randomUUID() + "@example.com", "password", PASSWORD)), null, 201);
        return new Account(response.path("user").path("id").asText(), response.path("accessToken").asText());
    }

    private JsonNode create(String path, String payload, Account account, int status) throws Exception {
        String response = mvc.perform(auth(post(path).contentType(MediaType.APPLICATION_JSON).content(payload), account))
                .andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return response.isBlank() ? json.createObjectNode() : body(response);
    }

    private JsonNode body(String value) { return json.readTree(value); }
    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, Account account) {
        return account == null ? request : request.header("Authorization", "Bearer " + account.token());
    }

    private String eventJson() {
        return """
                {"title":"Private","date":"%s","startTime":"09:00","endTime":"10:00","source":"internal","category":"work"}
                """.formatted(LocalDate.now());
    }

    private String signed(String subject, String issuer, Instant expiresAt) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().subject(subject).issuer(issuer).expiresAt(expiresAt).build())).getTokenValue();
    }
}
