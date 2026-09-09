package com.workspace.api.goals.controller;

import com.workspace.api.config.CorsConfig;
import com.workspace.api.goals.service.GoalsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalsController.class)
@Import(CorsConfig.class)
class GoalsCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalsService goalsService;

    @Test
    void allowsHomeToReadWeeklyGoals() throws Exception {
        given(goalsService.getAllGoals()).willReturn(List.of());

        mockMvc.perform(get("/goals")
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @CsvSource({"GET,/goals", "GET,/goals/1", "POST,/goals", "PUT,/goals/1", "DELETE,/goals/1"})
    void allowsAngularPreflightForWeeklyGoals(String method, String path) throws Exception {
        mockMvc.perform(options(path)
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", method)
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"));
    }
}
