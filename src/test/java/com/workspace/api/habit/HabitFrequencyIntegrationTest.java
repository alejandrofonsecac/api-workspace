package com.workspace.api.habit;

import com.workspace.api.pages.habit.repository.HabitRepository;
import com.workspace.api.support.AuthenticatedTest;
import com.workspace.api.user.domain.User;
import com.workspace.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(HabitFrequencyIntegrationTest.TimeConfiguration.class)
@Transactional
class HabitFrequencyIntegrationTest extends AuthenticatedTest {
    @TestConfiguration
    static class TimeConfiguration {
        @Bean @Primary Clock habitClock() {
            // Monday UTC is still Sunday in the product's configured time zone.
            return Clock.fixed(Instant.parse("2026-09-28T01:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }
    @Autowired MockMvc mvc;
    @Autowired HabitRepository habits;
    @Autowired UserRepository users;
    @Autowired ObjectMapper json;

    private String draft(String days) {
        return "{\"name\":\"Caminhar\",\"icon\":\"directions_run\",\"categoryId\":\"saude\",\"frequencyType\":\"custom\",\"daysOfWeek\":" + days + "}";
    }
    private int create() throws Exception {
        var response = mvc.perform(post("/habits").contentType(MediaType.APPLICATION_JSON).content(draft("[1,3,5]")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.asOfDate").value("2026-09-27"))
                .andExpect(jsonPath("$.history").isEmpty()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("id").asInt();
    }

    @Test void boardUsesScheduledDatesAndConfirmedCompletionsAndKeepsOwnerIsolation() throws Exception {
        int id = create();
        var habit = habits.findByIdAndUserId(id, testUser.getId()).orElseThrow();
        habit.setCreatedAt(LocalDate.parse("2026-09-21"));
        habits.saveAndFlush(habit);
        mvc.perform(post("/habit/{id}/completions/{date}/toggle", id, "2026-09-21")).andExpect(status().isOk());
        mvc.perform(post("/habit/{id}/completions/{date}/toggle", id, "2026-09-22")).andExpect(status().isConflict());
        mvc.perform(get("/habits/board"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.habits[0].history.length()").value(3))
                .andExpect(jsonPath("$.habits[0].history[0].date").value("2026-09-21"))
                .andExpect(jsonPath("$.habits[0].history[0].completed").value(true))
                .andExpect(jsonPath("$.habits[0].history[1].date").value("2026-09-23"))
                .andExpect(jsonPath("$.habits[0].history[1].completed").value(false))
                .andExpect(jsonPath("$.habits[0].metrics.consistency").value(33));
        User other = new User(); other.setName("Other"); other.setEmail(other.getId() + "@example.com"); other.setPasswordHash("unused"); users.saveAndFlush(other);
        mvc.perform(get("/habits/board").with(user(other.getId()))).andExpect(jsonPath("$.habits").isEmpty());
        mvc.perform(put("/habits/{id}", id).with(user(other.getId())).contentType(MediaType.APPLICATION_JSON).content(draft("[7]")))
                .andExpect(status().isNotFound());
    }

    @Test void editingFrequencyReturnsHistoryForTheUpdatedSchedule() throws Exception {
        int id = create();
        mvc.perform(put("/habits/{id}", id).contentType(MediaType.APPLICATION_JSON).content(draft("[7]")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.daysOfWeek[0]").value(7))
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.history[0].date").value("2026-09-27"));
    }

    @ParameterizedTest @ValueSource(strings = {"[]", "[0]", "[8]", "[null]", "null"})
    void rejectsInvalidCustomDaysWithoutSaving(String days) throws Exception {
        mvc.perform(post("/habits").contentType(MediaType.APPLICATION_JSON).content(draft(days))).andExpect(status().isBadRequest());
        assertThat(habits.findAllByUserId(testUser.getId())).isEmpty();
    }
}
