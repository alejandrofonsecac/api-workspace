package com.workspace.api.goals.controller;

import com.workspace.api.pages.goals.repository.GoalsRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GoalsControllerTest extends com.workspace.api.support.AuthenticatedTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GoalsRepository goalsRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createsReadsUpdatesAndDeletesGoal() throws Exception {

        //Config Test
        mockMvc.perform(post("/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Read books")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Read books"));

        Integer id = goalsRepository.findAll().getFirst().getId();
        mockMvc.perform(get("/goals/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(put("/goals/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Study Java")
                                .replace("10 books", "20 lessons")
                                .replace("NORMAL", "HIGH")
                                .replace("PERSONAL", "STUDY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Study Java"))
                .andExpect(jsonPath("$.expectedTarget").value("20 lessons"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.category").value("STUDY"));

        goalsRepository.flush();
        assertThat(goalsRepository.findById(id).orElseThrow().getGoalName()).isEqualTo("Study Java");

        mockMvc.perform(delete("/goals/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        goalsRepository.flush();
        assertThat(goalsRepository.existsById(id)).isFalse();
        mockMvc.perform(get("/goals/{id}", id)).andExpect(status().isNotFound());
    }

    //---Test---

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void returnsNotFoundForMissingGoal(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), "/goals/{id}", -1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Missing goal")))
                .andExpect(status().isNotFound());
    }

    @Test
    void persistsTheSelectedWeekAcrossReads() throws Exception {
        String body = goalJson("Read books")
                .replace("}", ",\"weekId\":\"2027-W01\"}");
        mockMvc.perform(post("/goals").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weekId").value("2027-W01"));

        flushAndClear();
        Integer id = goalsRepository.findAll().getFirst().getId();
        mockMvc.perform(get("/goals/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Read books"))
                .andExpect(jsonPath("$.weekId").value("2027-W01"));
        mockMvc.perform(get("/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].weekId").value("2027-W01"));

        mockMvc.perform(put("/goals/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Updated goal")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekId").value("2027-W01"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"weekId\":\"2027-W99\""})
    void rejectsInvalidWeeklyMetadata(String field) throws Exception {
        mockMvc.perform(post("/goals").contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Invalid").replace("}", "," + field + "}")))
                .andExpect(status().isBadRequest());
        assertThat(goalsRepository.count()).isZero();
    }

    private String goalJson(String name) {
        return """
                {"name":"%s","expectedTarget":"10 books","priority":"NORMAL",
                 "category":"PERSONAL"}
                """.formatted(name);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
