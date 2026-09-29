package com.workspace.api.retention;

import com.workspace.api.support.AuthenticatedTest;
import com.workspace.api.user.domain.User;
import com.workspace.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = RetentionTestConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "retention-integration"})
@Transactional
class RetentionApiTest extends AuthenticatedTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired com.workspace.api.event.repository.EventRepository events;

    private String payload(String route, String fields) {
        String base = route.equals("/events")
                ? "\"title\":\"Item\",\"date\":\"2030-01-01\",\"startTime\":\"09:00\",\"endTime\":\"10:00\",\"source\":\"internal\",\"category\":\"personal\""
                : "\"title\":\"Item\"";
        return "{" + base + (fields.isEmpty() ? "" : "," + fields) + "}";
    }

    @ParameterizedTest
    @ValueSource(strings = {"/events", "/notes"})
    void controlsCompletionOnServerAndPreservesRecurrenceAndOwnership(String route) throws Exception {
        String response = mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON)
                        .content(payload(route, "\"completed\":true,\"isRecurring\":true,\"recurrenceRule\":\"FREQ=DAILY\",\"completedAt\":\"2000-01-01T00:00:00Z\"")))
                .andExpect(status().is2xxSuccessful()).andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.isRecurring").value(true)).andReturn().getResponse().getContentAsString();
        var created = json.readTree(response);
        String id = created.path("id").asText();
        String completedAt = created.path("completedAt").asText();
        assertThat(completedAt).isNotBlank().isNotEqualTo("2000-01-01T00:00:00Z");
        mvc.perform(put(route + "/" + id).contentType(MediaType.APPLICATION_JSON).content(payload(route, "")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completedAt").value(completedAt))
                .andExpect(jsonPath("$.isRecurring").value(true)).andExpect(jsonPath("$.recurrenceRule").value("FREQ=DAILY"));
        mvc.perform(put(route + "/" + id).contentType(MediaType.APPLICATION_JSON).content(payload(route, "\"completed\":false")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(false)).andExpect(jsonPath("$.completedAt").isEmpty());
        mvc.perform(put(route + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(payload(route, "\"completed\":true,\"isRecurring\":false,\"recurrenceRule\":\"\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.isRecurring").value(false)).andExpect(jsonPath("$.recurrenceRule").isEmpty());
        User other = new User();
        other.setName("Other"); other.setEmail(other.getId() + "@example.com"); other.setPasswordHash("unused");
        users.saveAndFlush(other);
        mvc.perform(put(route + "/" + id).with(user(other.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content(payload(route, "\"completed\":false")))
                .andExpect(status().isNotFound());
    }

    @Test
    void futureConflictRejectsEntireSeries() throws Exception {
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
                .content(payload("/events", "").replace("2030-01-01", "2030-01-08"))).andExpect(status().isCreated());
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
                .content(payload("/events", "\"isRecurring\":true,\"recurrenceRule\":\"FREQ=WEEKLY;UNTIL=20300115\"")))
                .andExpect(status().isConflict());
        assertThat(events.findAllByUserId(testUser.getId())).hasSize(1);
    }

    @Test
    void eventsPersistAllDatesWithIndependentCompletion() throws Exception {
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
                .content(payload("/events", "\"completed\":true,\"isRecurring\":true,\"recurrenceRule\":\"FREQ=WEEKLY;UNTIL=20300115\"")))
                .andExpect(status().isCreated());
        var list = events.findByUserIdOrderByDateAscStartTimeAsc(testUser.getId());
        assertThat(list).hasSize(3);
        assertThat(list.get(0).getCompleted()).isTrue();
        assertThat(list.get(1).getCompleted()).isFalse();
        assertThat(list.get(1).getCompletedAt()).isNull();
        assertThat(list.get(2).getDate()).isEqualTo(java.time.LocalDate.parse("2030-01-15"));
        mvc.perform(get("/calendar/events"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].completed").value(true))
                .andExpect(jsonPath("$[1].completed").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/events"})
    void rejectsInvalidRecurrenceBeforeWriting(String route) throws Exception {
        mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON)
                .content(payload(route, "\"dueDate\":\"2030-01-01\",\"isRecurring\":true,\"recurrenceRule\":\"FREQ=WEEKLY;UNTIL=20300230\"")))
                .andExpect(status().isBadRequest());
        assertThat(events.findAllByUserId(testUser.getId())).isEmpty();
    }
}
