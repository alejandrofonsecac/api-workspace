package com.workspace.api.goals.controller;

import com.workspace.api.goals.entity.Goals;
import com.workspace.api.goals.repository.GoalsRepository;
import com.workspace.api.habit.entity.Habit;
import com.workspace.api.habit.entity.Category;
import com.workspace.api.habit.repository.HabitRepository;
import com.workspace.api.util.domain.Priority;
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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GoalsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GoalsRepository goalsRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createsReadsUpdatesAndDeletesGoalWithoutDeletingHabits() throws Exception {
        Habit originalHabit = saveHabit("Read");
        Habit newHabit = saveHabit("Study");

        //Config Test
        mockMvc.perform(post("/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Read books", originalHabit.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Read books"));

        Integer id = goalsRepository.findAll().getFirst().getId();
        mockMvc.perform(get("/goals/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.habitId").value(originalHabit.getId()));

        mockMvc.perform(put("/goals/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Study Java", newHabit.getId())
                                .replace("10 books", "20 lessons")
                                .replace("NORMAL", "HIGH")
                                .replace("PERSONAL", "STUDY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Study Java"))
                .andExpect(jsonPath("$.expectedTarget").value("20 lessons"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.category").value("STUDY"))
                .andExpect(jsonPath("$.habitId").value(newHabit.getId()));

        goalsRepository.flush();
        assertThat(goalsRepository.findById(id).orElseThrow().getGoalName()).isEqualTo("Study Java");

        mockMvc.perform(delete("/goals/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        goalsRepository.flush();
        assertThat(goalsRepository.existsById(id)).isFalse();
        assertThat(habitRepository.existsById(originalHabit.getId())).isTrue();
        assertThat(habitRepository.existsById(newHabit.getId())).isTrue();
        mockMvc.perform(get("/goals/{id}", id)).andExpect(status().isNotFound());
    }

    //---Test---

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void returnsNotFoundForMissingGoal(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), "/goals/{id}", -1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Missing goal", -1)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsMissingHabitWithoutChangingGoal(String method) throws Exception {
        Goals goal = saveGoal();
        String path = method.equals("POST") ? "/goals" : "/goals/" + goal.getId();

        mockMvc.perform(request(HttpMethod.valueOf(method), path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Changed", -1)))
                .andExpect(status().isNotFound());

        assertThat(goal.getGoalName()).isEqualTo("Original");
        assertThat(goalsRepository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createsReadsUpdatesAndDeletesGoalWithoutHabit(boolean explicitNull) throws Exception {
        mockMvc.perform(post("/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalWithoutHabitJson("Read books", explicitNull)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Read books"))
                .andExpect(jsonPath("$.habitId").value(nullValue()));

        flushAndClear();
        Goals savedGoal = goalsRepository.findAll().getFirst();
        Integer id = savedGoal.getId();
        assertThat(savedGoal.getHabit()).isNull();

        mockMvc.perform(get("/goals/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.habitId").value(nullValue()));

        mockMvc.perform(get("/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].habitId").value(nullValue()));

        mockMvc.perform(put("/goals/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalWithoutHabitJson("Study Java", explicitNull)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Study Java"))
                .andExpect(jsonPath("$.habitId").value(nullValue()));

        flushAndClear();
        Goals updatedGoal = goalsRepository.findById(id).orElseThrow();
        assertThat(updatedGoal.getGoalName()).isEqualTo("Study Java");
        assertThat(updatedGoal.getHabit()).isNull();

        mockMvc.perform(delete("/goals/{id}", id))
                .andExpect(status().isNoContent());

        flushAndClear();
        assertThat(goalsRepository.existsById(id)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void removesAndRestoresHabitOnUpdate(boolean explicitNull) throws Exception {
        Goals goal = saveGoal();
        Integer id = goal.getId();
        Integer habitId = goal.getHabit().getId();

        mockMvc.perform(put("/goals/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalWithoutHabitJson("Changed", explicitNull)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habitId").value(nullValue()));

        flushAndClear();
        assertThat(goalsRepository.findById(id).orElseThrow().getHabit()).isNull();
        assertThat(habitRepository.existsById(habitId)).isTrue();

        mockMvc.perform(put("/goals/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Linked again", habitId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habitId").value(habitId));

        flushAndClear();
        assertThat(goalsRepository.findById(id).orElseThrow().getHabit().getId()).isEqualTo(habitId);
    }

    private Habit saveHabit(String name) {
        Habit habit = new Habit();
        habit.setName(name);
        return habitRepository.save(habit);
    }

    @Test
    void persistsTheSelectedWeekAcrossReads() throws Exception {
        Habit habit = saveHabit("Read");
        String body = goalJson("Read books", habit.getId())
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
                        .content(goalJson("Without habit", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekId").value("2027-W01"))
                .andExpect(jsonPath("$.habitId").value(nullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"weekId\":\"2027-W99\""})
    void rejectsInvalidWeeklyMetadata(String field) throws Exception {
        mockMvc.perform(post("/goals").contentType(MediaType.APPLICATION_JSON)
                        .content(goalJson("Invalid", null).replace("}", "," + field + "}")))
                .andExpect(status().isBadRequest());
        assertThat(goalsRepository.count()).isZero();
    }

    private Goals saveGoal() {
        Goals goal = new Goals();
        goal.setGoalName("Original");
        goal.setExpectedTarget("10 books");
        goal.setPriority(Priority.NORMAL);
        goal.setCategory(Category.PERSONAL);
        goal.setWeekId("2026-W37");
        goal.setHabit(saveHabit("Read"));
        return goalsRepository.save(goal);
    }

    private String goalJson(String name, Integer habitId) {
        return """
                {"name":"%s","expectedTarget":"10 books","priority":"NORMAL",
                 "category":"PERSONAL","habitId":%s}
                """.formatted(name, habitId);
    }

    private String goalWithoutHabitJson(String name, boolean explicitNull) {
        String json = goalJson(name, null);
        return explicitNull ? json : json.replace(",\"habitId\":null", "");
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
