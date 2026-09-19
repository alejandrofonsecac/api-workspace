package com.workspace.api.goals.controller;

import com.workspace.api.util.domain.Category;
import com.workspace.api.util.domain.Priority;
import com.workspace.api.goals.entity.Goals;
import com.workspace.api.goals.repository.GoalsRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GoalsRequestValidationTest extends com.workspace.api.support.AuthenticatedTest {

    private static final String VALID_GOAL = """
            {
              "name": "Estudar Java",
              "expectedTarget": "Concluir cinco aulas",
              "priority": "HIGH",
              "category": "STUDY"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GoalsRepository goalsRepository;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest
    @ValueSource(strings = {
            "\"name\": \"Estudar Java\"",
            "\"expectedTarget\": \"Concluir cinco aulas\"",
            "\"priority\": \"HIGH\"",
            "\"category\": \"STUDY\""
    })
    void stillRequiresGoalDetails(String field) throws Exception {
        String missingField = field.substring(0, field.indexOf(':') + 1) + " null";

        mockMvc.perform(post("/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_GOAL.replace(field, missingField)))
                .andExpect(status().isBadRequest());

        assertThat(goalsRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsFrontendFieldNamesWithoutPersistingIncompleteGoal(String method) throws Exception {
        Goals original = saveGoal();
        String path = method.equals("POST") ? "/goals" : "/goals/" + original.getId();

        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Estudar Java","expectedResult":"Concluir cinco aulas",
                                 "priority":"NORMAL","categoryId":"estudo"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("name is required")))
                .andExpect(jsonPath("$.message").value(containsString("expectedTarget is required")))
                .andExpect(jsonPath("$.message").value(containsString("category is required")));

        flushAndClear();
        assertThat(goalsRepository.count()).isEqualTo(1);
        Goals persisted = goalsRepository.findById(original.getId()).orElseThrow();
        assertThat(persisted.getGoalName()).isEqualTo("Estudar Java");
        assertThat(persisted.getCategory()).isEqualTo(Category.STUDY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsNullCategoryBeforeWritingToDatabase(String method) throws Exception {
        Goals original = saveGoal();
        String path = method.equals("POST") ? "/goals" : "/goals/" + original.getId();

        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_GOAL.replace("\"category\": \"STUDY\"", "\"category\": null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("category is required"));

        flushAndClear();
        assertThat(goalsRepository.count()).isEqualTo(1);
        assertThat(goalsRepository.findById(original.getId()).orElseThrow().getCategory()).isEqualTo(Category.STUDY);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\" \",\"expectedTarget\":\" \",\"priority\":\"NORMAL\",\"category\":\"STUDY\"}",
            "{}"
    })
    void rejectsBlankOrMissingDetails(String body) throws Exception {
        mockMvc.perform(post("/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("name is required")))
                .andExpect(jsonPath("$.message").value(containsString("expectedTarget is required")));

        assertThat(goalsRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"HIGH", "STUDY"})
    void rejectsInvalidEnumsWithGoalSpecificMessage(String value) throws Exception {
        mockMvc.perform(post("/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_GOAL.replace(value, "INVALID")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid goal request")));

        assertThat(goalsRepository.count()).isZero();
    }

    @Test
    void deletesGoal() throws Exception {
        Goals goal = saveGoal();

        mockMvc.perform(delete("/goals/{id}", goal.getId()))
                .andExpect(status().isNoContent());

        flushAndClear();
        assertThat(goalsRepository.existsById(goal.getId())).isFalse();
    }

    private Goals saveGoal() {
        Goals goal = new Goals();
        goal.setUserId(testUser.getId());
        goal.setGoalName("Estudar Java");
        goal.setExpectedTarget("Concluir cinco aulas");
        goal.setPriority(Priority.HIGH);
        goal.setCategory(Category.STUDY);
        return goalsRepository.saveAndFlush(goal);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
