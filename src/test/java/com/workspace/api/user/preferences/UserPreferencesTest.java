package com.workspace.api.user.preferences;

import com.workspace.api.support.AuthenticatedTest;
import com.workspace.api.user.domain.User;
import com.workspace.api.user.preferences.repository.UserPreferencesRepository;
import com.workspace.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserPreferencesTest extends AuthenticatedTest {
    private static final String PATH = "/users/me/preferences";
    private static final String DARK = """
            {"theme":"dark","accent":"teal","plannerDisplay":"radial",
             "sleep":{"enabled":true,"start":22,"end":6},"sidebarWidth":320}
            """;

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserPreferencesRepository preferences;

    @Test
    void returnsDefaultsBeforeFirstSave() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("light"))
                .andExpect(jsonPath("$.accent").value("blue"))
                .andExpect(jsonPath("$.plannerDisplay").value("compact"))
                .andExpect(jsonPath("$.sleep.enabled").value(false))
                .andExpect(jsonPath("$.sleep.start").value(23))
                .andExpect(jsonPath("$.sleep.end").value(7))
                .andExpect(jsonPath("$.sidebarWidth").value(280))
                .andExpect(jsonPath("$.updatedAt").value(nullValue()));
        assertThat(preferences.existsById(testUser.getId())).isFalse();
    }

    @Test
    void savesAndReplacesPreferencesOfTheAuthenticatedUser() throws Exception {
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(DARK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()));
        mvc.perform(get(PATH))
                .andExpect(jsonPath("$.accent").value("teal"))
                .andExpect(jsonPath("$.plannerDisplay").value("radial"))
                .andExpect(jsonPath("$.sleep.enabled").value(true))
                .andExpect(jsonPath("$.sleep.start").value(22))
                .andExpect(jsonPath("$.sleep.end").value(6))
                .andExpect(jsonPath("$.sidebarWidth").value(320));

        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(DARK.replace("\"dark\"", "\"system\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.theme").value("system"));
        assertThat(preferences.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"theme\":\"dark\",\"theme\":\"neon\"",
            "\"accent\":\"teal\",\"accent\":\"red\"",
            "\"plannerDisplay\":\"radial\",\"plannerDisplay\":\"grid\"",
            "\"start\":22,\"start\":24",
            "\"end\":6,\"end\":-1",
            "\"sidebarWidth\":320,\"sidebarWidth\":239",
            "\"sidebarWidth\":320,\"sidebarWidth\":341",
            "\"sleep\":{\"enabled\":true,\"start\":22,\"end\":6},\"sleep\":null",
    })
    void rejectsInvalidValuesWithoutSaving(String replacement) throws Exception {
        String[] parts = replacement.split(",(?=\")", 2);
        String body = DARK.replace(parts[0], parts[1]);
        assertThat(body).isNotEqualTo(DARK);
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        assertThat(preferences.existsById(testUser.getId())).isFalse();
    }

    @Test
    void rejectsMissingFieldsAndMalformedJson() throws Exception {
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"theme\":\"dark\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ignoresUserIdSentByTheClientAndIsolatesAccounts() throws Exception {
        User other = new User();
        other.setName("Other");
        other.setEmail(other.getId() + "@example.com");
        other.setPasswordHash("unused-in-mock-authentication");
        users.saveAndFlush(other);

        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(DARK.replace("{\"theme\"", "{\"userId\":\"" + other.getId() + "\",\"theme\"")))
                .andExpect(status().isOk());
        assertThat(preferences.existsById(testUser.getId())).isTrue();
        assertThat(preferences.existsById(other.getId())).isFalse();

        var otherAuthentication = new UsernamePasswordAuthenticationToken(
                other.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        mvc.perform(get(PATH).with(authentication(otherAuthentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("light"))
                .andExpect(jsonPath("$.updatedAt").value(nullValue()));
    }

    @Test
    void requiresAuthentication() throws Exception {
        TestSecurityContextHolder.clearContext();
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(DARK))
                .andExpect(status().isUnauthorized());
    }
}
