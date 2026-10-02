package com.workspace.api.goals.controller;

import com.workspace.api.pages.goals.entity.Goals;
import com.workspace.api.pages.goals.repository.GoalsRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GoalsLifecycleTest.TimeConfiguration.class)
@Transactional
class GoalsLifecycleTest extends com.workspace.api.support.AuthenticatedTest {
    @TestConfiguration
    static class TimeConfiguration {
        @Bean @Primary Clock fixedClock() {
            // Monday in UTC, still Sunday in the configured application time zone.
            return Clock.fixed(Instant.parse("2027-01-04T01:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }
    @Autowired MockMvc mvc;
    @Autowired GoalsRepository goals;
    @Autowired EntityManager em;

    @Test void serverOwnsDefaultsDatesAndStatusAndIgnoresClientLifecycleFields() throws Exception {
        mvc.perform(post("/goals").contentType(MediaType.APPLICATION_JSON).content(payload(null)
                        .replace("}", ",\"status\":\"COMPLETED\",\"completedAt\":\"2000-01-01T00:00:00Z\",\"deferredCount\":99}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weekId").value("2026-W53"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.createdAt").value("2027-01-04T01:00:00Z"))
                .andExpect(jsonPath("$.completedAt").value(nullValue()))
                .andExpect(jsonPath("$.deferredCount").value(0));
        mvc.perform(get("/goals/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.week.id").value("2026-W53"))
                .andExpect(jsonPath("$.week.nextId").value("2027-W01"))
                .andExpect(jsonPath("$.week.start").value("2026-12-28"))
                .andExpect(jsonPath("$.week.end").value("2027-01-03"))
                .andExpect(jsonPath("$.defaults.priority").value("NORMAL"));
    }

    @Test void completionIsPersistentIdempotentAndPreventsDeferral() throws Exception {
        int id = create(null);
        mvc.perform(post("/goals/{id}/complete", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        reload();
        mvc.perform(get("/goals/{id}", id))
                .andExpect(jsonPath("$.completedAt").value("2027-01-04T01:00:00Z"))
                .andExpect(jsonPath("$.canComplete").value(false))
                .andExpect(jsonPath("$.canDefer").value(false));
        mvc.perform(post("/goals/{id}/complete", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").value("2027-01-04T01:00:00Z"));
        mvc.perform(post("/goals/{id}/defer", id)).andExpect(status().isConflict());
        assertThat(goals.findById(id).orElseThrow().getDeferredCount()).isZero();
    }

    @Test void deferralCrossesTheIsoYearAndPersistsCountWithoutChangingCreationDate() throws Exception {
        int id = create("2026-W53");
        mvc.perform(post("/goals/{id}/defer", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.weekId").value("2027-W01"))
                .andExpect(jsonPath("$.status").value("PLANNED"));
        reload();
        mvc.perform(get("/goals/{id}", id))
                .andExpect(jsonPath("$.deferredCount").value(1))
                .andExpect(jsonPath("$.createdAt").value("2027-01-04T01:00:00Z"));
        mvc.perform(get("/goals/board")).andExpect(jsonPath("$.goals.length()").value(0));
        mvc.perform(get("/goals/board").param("weekId", "2027-W01"))
                .andExpect(jsonPath("$.goals[0].id").value(id));
    }

    @Test void updateCannotBypassDeferralAndKeepsCompletion() throws Exception {
        int id = create(null);
        mvc.perform(put("/goals/{id}", id).contentType(MediaType.APPLICATION_JSON).content(payload("2027-W01")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/goals/{id}/complete", id)).andExpect(status().isOk());
        mvc.perform(put("/goals/{id}", id).contentType(MediaType.APPLICATION_JSON).content(payload(null)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test void filteringAndWholeWeekSummaryComeFromBackend() throws Exception {
        int id = create(null);
        mvc.perform(post("/goals/{id}/complete", id)).andExpect(status().isOk());
        create(null);
        create("2027-W01");
        mvc.perform(get("/goals/board").param("status", "COMPLETED").param("category", "STUDY"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.goals.length()").value(1))
                .andExpect(jsonPath("$.summary.total").value(2))
                .andExpect(jsonPath("$.summary.completed").value(1))
                .andExpect(jsonPath("$.summary.percentage").value(50));
        mvc.perform(get("/goals/board").param("category", "HEALTH"))
                .andExpect(jsonPath("$.goals.length()").value(0))
                .andExpect(jsonPath("$.summary.total").value(2));
    }

    @ParameterizedTest @ValueSource(strings = {"2027-W53", "2026-W00", "2026-W99", "invalid"})
    void invalidWeeksReturn400(String week) throws Exception {
        mvc.perform(get("/goals/board").param("weekId", week)).andExpect(status().isBadRequest());
        mvc.perform(post("/goals").contentType(MediaType.APPLICATION_JSON).content(payload(week)))
                .andExpect(status().isBadRequest());
    }

    @Test void invalidFiltersAndMissingCommandsReturnClientErrors() throws Exception {
        mvc.perform(get("/goals/board").param("category", "invalid")).andExpect(status().isBadRequest());
        mvc.perform(get("/goals/board").param("status", "invalid")).andExpect(status().isBadRequest());
        mvc.perform(post("/goals/-1/complete")).andExpect(status().isNotFound());
        mvc.perform(post("/goals/-1/defer")).andExpect(status().isNotFound());
    }

    @Test void focusSurvivesReloadAndBelongsToTheSelectedWeek() throws Exception {
        mvc.perform(put("/goals/focus/2026-W53").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"focus\":\"  Study  \",\"message\":\"  Keep going  \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.focus").value("Study"));
        reload();
        mvc.perform(get("/goals/board")).andExpect(jsonPath("$.focus.message").value("Keep going"));
        mvc.perform(get("/goals/board").param("weekId", "2027-W01"))
                .andExpect(jsonPath("$.focus").value(nullValue()));
        mvc.perform(put("/goals/focus/2026-W53").contentType(MediaType.APPLICATION_JSON).content("{\"focus\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    private int create(String week) throws Exception {
        mvc.perform(post("/goals").contentType(MediaType.APPLICATION_JSON).content(payload(week)))
                .andExpect(status().isCreated());
        return goals.findAll().stream().mapToInt(Goals::getId).max().orElseThrow();
    }
    private String payload(String week) {
        return """
                {"name":"Study","expectedTarget":"Five lessons","priority":"NORMAL","category":"STUDY"%s}
                """.formatted(week == null ? "" : ",\"weekId\":\"" + week + "\"");
    }
    private void reload() { em.flush(); em.clear(); }
}
