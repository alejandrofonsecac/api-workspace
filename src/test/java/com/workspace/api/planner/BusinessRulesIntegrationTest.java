package com.workspace.api.planner;

import com.workspace.api.pages.event.repository.EventRepository;
import com.workspace.api.user.domain.User;
import com.workspace.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BusinessRulesIntegrationTest extends com.workspace.api.support.AuthenticatedTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EventRepository events;
    @Autowired UserRepository users;
    @Autowired Clock clock;

    @Test
    void acceptsMinuteIntervalsAndRejectsOverlapAndReverseRanges() throws Exception {
        String date = LocalDate.now(clock).toString();
        String event = "{\"title\":\"Morning\",\"date\":\"" + date + "\",\"startTime\":\"06:15\",\"endTime\":\"07:30\",\"source\":\"internal\",\"category\":\"personal\"}";
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(event)).andExpect(status().isCreated());
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(event)).andExpect(status().isConflict());
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(event.replace("07:30", "05:00")))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/planner/summary").param("date", date))
                .andExpect(jsonPath("$.availability.occupiedBlocks").value(2));
    }

    @Test
    void habitsCrudMetricsAndDashboardUseServerState() throws Exception {
        String draft = "{\"name\":\"Read\",\"icon\":\"auto_stories\",\"categoryId\":\"estudo\",\"frequencyType\":\"daily\",\"goal\":\"One chapter\",\"note\":\"\"}";
        String response = mvc.perform(post("/habits").contentType(MediaType.APPLICATION_JSON).content(draft))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.metrics.consistency").value(0))
                .andReturn().getResponse().getContentAsString();
        int id = json.readTree(response).path("id").asInt();
        mvc.perform(post("/habit/{id}/completions/{date}/toggle", id, LocalDate.now(clock))).andExpect(status().isOk());
        mvc.perform(get("/habits/board")).andExpect(jsonPath("$.summary.highestCurrentStreak").value(1))
                .andExpect(jsonPath("$.habits[0].metrics.consistency").value(100));
        mvc.perform(put("/habits/{id}", id).contentType(MediaType.APPLICATION_JSON).content(draft.replace("Read", "Read more")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Read more"));
        createEvent(LocalDate.now(clock).toString());
        mvc.perform(get("/home/dashboard")).andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalEvents").value(1))
                .andExpect(jsonPath("$.summary.highestCurrentStreak").value(1))
                .andExpect(jsonPath("$.habits[0].progress").value(100));
        mvc.perform(put("/habits/{id}/status", id).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"paused\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/habit/planner").param("date", LocalDate.now(clock).toString())).andExpect(content().json("[]"));
        mvc.perform(post("/habit/{id}/completions/{date}/toggle", id, LocalDate.now(clock))).andExpect(status().isConflict());
        mvc.perform(delete("/habits/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/habits/board")).andExpect(jsonPath("$.summary.activeCount").value(0));
    }

    @Test
    void otherUsersCannotUpdateNotesHabitsOrUpdateEvents() throws Exception {
        User other = new User(); other.setName("Other"); other.setEmail(other.getId()+"@example.com"); other.setPasswordHash("test"); users.saveAndFlush(other);
        String note = mvc.perform(post("/notes").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Private\"}"))
                .andReturn().getResponse().getContentAsString();
        String noteId = json.readTree(note).path("id").asText();
        mvc.perform(put("/notes/{id}",noteId).contentType(MediaType.APPLICATION_JSON).content("{\"favourite\":true,\"fixed\":true,\"edited\":\"2000-01-01T00:00:00\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.favourite").value(true))
                .andExpect(jsonPath("$.edited").value(org.hamcrest.Matchers.not("2000-01-01T00:00:00")));
        mvc.perform(put("/notes/{id}",noteId).with(user(other.getId())).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Stolen\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/home/dashboard").with(user(other.getId())))
                .andExpect(jsonPath("$.summary.totalEvents").value(0)).andExpect(jsonPath("$.planner.events").isEmpty());
        String habit = mvc.perform(post("/habits").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Private\",\"icon\":\"spa\",\"categoryId\":\"saude\",\"frequencyType\":\"daily\"}"))
                .andReturn().getResponse().getContentAsString();
        int habitId=json.readTree(habit).path("id").asInt();
        mvc.perform(delete("/habits/{id}",habitId).with(user(other.getId()))).andExpect(status().isNotFound());
        mvc.perform(put("/habits/{id}/status",habitId).with(user(other.getId())).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"paused\"}"))
                .andExpect(status().isNotFound());
    }

    private long createEvent(String date) throws Exception {
        var response=mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test event\",\"date\":\""+date+"\",\"source\":\"internal\",\"category\":\"personal\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("id").asLong();
    }

}
