package com.workspace.api.event.controller;

import com.workspace.api.support.AuthenticatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventUnificationTest extends AuthenticatedTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private static final String DRAFT = "{\"title\":\"Read\",\"source\":\"internal\",\"category\":\"personal\"}";

    @Test void undatedEventCanBeScheduledCompletedAndReopened() throws Exception {
        String draft = DRAFT.replace("}", ",\"priority\":\"Alta\",\"completed\":true,\"completedAt\":\"2000-01-01T00:00:00Z\"}");
        var response = mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(draft))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.date").isEmpty())
                .andExpect(jsonPath("$.startTime").isEmpty()).andExpect(jsonPath("$.endTime").isEmpty())
                .andExpect(jsonPath("$.priority").value("Alta")).andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").value(org.hamcrest.Matchers.not("2000-01-01T00:00:00Z")))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(response).path("id").asLong();
        mvc.perform(get("/calendar/events")).andExpect(content().json("[]"));
        mvc.perform(put("/events/"+id).contentType(MediaType.APPLICATION_JSON)
                .content(DRAFT.replace("}", ",\"date\":\"2030-01-01\",\"startTime\":\"09:00\",\"completed\":false}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.completedAt").isEmpty()).andExpect(jsonPath("$.priority").isEmpty());
        mvc.perform(get("/planner/availability").param("date","2030-01-01"))
                .andExpect(jsonPath("$.availableBlocks").value(24));
        mvc.perform(get("/calendar/events")).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(put("/events/"+id).contentType(MediaType.APPLICATION_JSON).content(DRAFT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.date").isEmpty());
    }

    @Test void rejectsOrphanTimesAndInvalidPriority() throws Exception {
        for (String fields : new String[]{"\"startTime\":\"09:00\"", "\"date\":\"2030-01-01\",\"endTime\":\"10:00\"", "\"priority\":\"urgent\""}) {
            mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(DRAFT.replace("}", ","+fields+"}")))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Test void pointAndDateOnlyEventsDoNotBlockIntervals() throws Exception {
        for (String fields : new String[]{"", ",\"startTime\":\"09:30\"", ",\"startTime\":\"09:00\",\"endTime\":\"10:00\""}) {
            mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
                    .content(DRAFT.replace("}", ",\"date\":\"2030-01-01\""+fields+"}"))).andExpect(status().isCreated());
        }
        mvc.perform(get("/planner/summary").param("date","2030-01-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.eventCount").value(3))
                .andExpect(jsonPath("$.availability.occupiedBlocks").value(1));
    }
}
