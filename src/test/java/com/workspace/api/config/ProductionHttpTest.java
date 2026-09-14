package com.workspace.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"production", "test"})
class ProductionHttpTest {
    private static final String SITE = "https://plannerfy-production.jfonsecacorrea08.chatgpt.site";

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @CsvSource({"GET,/weather", "POST,/goals", "PUT,/goals/1", "DELETE,/notes/1",
            "POST,/habit", "POST,/event", "POST,/events", "PUT,/schedule/tasks/1",
            "POST,/planner/events", "GET,/planner/availability"})
    void allowsSitePreflightAcrossApi(String method, String path) throws Exception {
        mvc.perform(options(path).header("Origin", SITE)
                        .header("Access-Control-Request-Method", method)
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", SITE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:4200", "https://untrusted.example"})
    void rejectsOtherOrigins(String origin) throws Exception {
        mvc.perform(options("/goals").header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void allowsActualSiteRequest() throws Exception {
        mvc.perform(get("/notes").header("Origin", SITE))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", SITE));
    }

    @Test
    void exposesHealthWithoutInternalDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }
}
