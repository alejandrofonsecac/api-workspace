package com.workspace.api.home.weather.controller;

import com.workspace.api.config.CorsConfig;
import com.workspace.api.pages.home.weather.controller.WeatherController;
import com.workspace.api.pages.home.weather.dto.WeatherResponseDTO;
import com.workspace.api.pages.home.weather.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeatherController.class)
@Import({CorsConfig.class, com.workspace.api.user.security.config.SecurityConfiguration.class})
@org.springframework.security.test.context.support.WithMockUser
class WeatherControllerTest {
    @MockitoBean com.workspace.api.user.repository.UserRepository users;
    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;
    @MockitoBean com.workspace.api.user.security.RefreshCookie refreshCookie;
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherService weatherService;

    @Test
    void allowsAngularToReadWeather() throws Exception {
        given(weatherService.getWeather(-23.5505, -46.6333))
                .willReturn(new WeatherResponseDTO(22.4, "São Paulo", "Chuva"));

        mockMvc.perform(get("/weather")
                        .param("latitude", "-23.5505")
                        .param("longitude", "-46.6333")
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(jsonPath("$.temperature").value(22.4))
                .andExpect(jsonPath("$.city").value("São Paulo"))
                .andExpect(jsonPath("$.condition").value("Chuva"));
    }
}
