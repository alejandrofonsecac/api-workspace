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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserPreferencesTest extends AuthenticatedTest {
    private static final String PATH = "/users/me/preferences";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserPreferencesRepository preferences;

    private ResultActions patchJson(String body) throws Exception {
        return mvc.perform(patch(PATH).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void returnsAnEmptyDocumentBeforeTheFirstSave() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences", aMapWithSize(0)))
                .andExpect(jsonPath("$.updatedAt").value(nullValue()));
        assertThat(preferences.existsById(testUser.getId())).isFalse();
    }

    @Test
    void storesKeysTheBackendDoesNotKnowAndReturnsThemUnchanged() throws Exception {
        patchJson("""
                {"theme":"dark","sidebarWidth":320,"sleep":{"enabled":true,"start":22,"end":6},
                 "someFutureSetting":["a","b"]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences.theme").value("dark"))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()));

        mvc.perform(get(PATH))
                .andExpect(jsonPath("$.preferences.theme").value("dark"))
                .andExpect(jsonPath("$.preferences.sidebarWidth").value(320))
                .andExpect(jsonPath("$.preferences.sleep.enabled").value(true))
                .andExpect(jsonPath("$.preferences.sleep.start").value(22))
                .andExpect(jsonPath("$.preferences.sleep.end").value(6))
                .andExpect(jsonPath("$.preferences.someFutureSetting[1]").value("b"));
    }

    @Test
    void mergesChangesWithoutLosingKeysThatWereNotSent() throws Exception {
        patchJson("{\"theme\":\"dark\",\"accent\":\"teal\",\"sleep\":{\"enabled\":true,\"start\":22,\"end\":6}}")
                .andExpect(status().isOk());

        // Um cliente que só conhece "theme" não apaga "accent"; objetos são substituídos por inteiro.
        patchJson("{\"theme\":\"system\",\"sleep\":{\"enabled\":false}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences.theme").value("system"))
                .andExpect(jsonPath("$.preferences.accent").value("teal"))
                .andExpect(jsonPath("$.preferences.sleep", aMapWithSize(1)));

        patchJson("{\"accent\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences.accent").doesNotExist())
                .andExpect(jsonPath("$.preferences.theme").value("system"));
        assertThat(preferences.count()).isEqualTo(1);
    }

    @Test
    void anEmptyPatchDoesNotCreateADocument() throws Exception {
        patchJson("{}").andExpect(status().isOk()).andExpect(jsonPath("$.updatedAt").value(nullValue()));
        assertThat(preferences.existsById(testUser.getId())).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[]",
            "\"dark\"",
            "{",
            "{\"Theme\":\"dark\"}",
            "{\"the-me\":\"dark\"}",
            "{\"sleep\":{\"Start\":1}}",
            "{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":1}}}}}",
    })
    void rejectsDocumentsOutsideTheStructuralLimits(String body) throws Exception {
        patchJson(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        assertThat(preferences.existsById(testUser.getId())).isFalse();
    }

    @Test
    void rejectsLongStringsTooManyKeysAndOversizedDocuments() throws Exception {
        patchJson("{\"note\":\"" + "x".repeat(501) + "\"}").andExpect(status().isBadRequest());

        StringBuilder manyKeys = new StringBuilder("{");
        for (int i = 0; i < 101; i++) {
            manyKeys.append(i == 0 ? "" : ",").append("\"k").append(i).append("\":1");
        }
        patchJson(manyKeys.append("}").toString()).andExpect(status().isBadRequest());

        StringBuilder large = new StringBuilder("{");
        for (int i = 0; i < 40; i++) {
            large.append(i == 0 ? "" : ",").append("\"k").append(i).append("\":\"").append("x".repeat(450)).append("\"");
        }
        patchJson(large.append("}").toString()).andExpect(status().isBadRequest());
        assertThat(preferences.existsById(testUser.getId())).isFalse();
    }

    @Test
    void ignoresUserIdSentByTheClientAndIsolatesAccounts() throws Exception {
        User other = new User();
        other.setName("Other");
        other.setEmail(other.getId() + "@example.com");
        other.setPasswordHash("unused-in-mock-authentication");
        users.saveAndFlush(other);

        // "userId" é apenas mais uma chave do documento do usuário autenticado.
        patchJson("{\"theme\":\"dark\",\"userId\":\"" + other.getId() + "\"}").andExpect(status().isOk());
        assertThat(preferences.existsById(testUser.getId())).isTrue();
        assertThat(preferences.existsById(other.getId())).isFalse();

        var otherAuthentication = new UsernamePasswordAuthenticationToken(
                other.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        mvc.perform(get(PATH).with(authentication(otherAuthentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferences", aMapWithSize(0)))
                .andExpect(jsonPath("$.updatedAt").value(nullValue()));
    }

    @Test
    void requiresAuthentication() throws Exception {
        TestSecurityContextHolder.clearContext();
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        patchJson("{\"theme\":\"dark\"}").andExpect(status().isUnauthorized());
    }
}
