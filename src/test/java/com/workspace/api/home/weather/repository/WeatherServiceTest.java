package com.workspace.api.home.weather.repository;

import com.workspace.api.pages.home.weather.dto.WeatherResponseDTO;
import com.workspace.api.pages.home.weather.service.WeatherService;
import com.workspace.api.pages.home.weather.validation.WeatherValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WeatherServiceTest {
    private MockRestServiceServer server;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.createServer(builder);
        weatherService = new WeatherService(builder, new WeatherValidator());
    }

    @Test
    void getsCurrentWeatherAndResolvesTheCityFromNominatim() {
        server.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("latitude", "-23.5505"))
                .andExpect(queryParam("longitude", "-46.6333"))
                .andExpect(queryParam("current", "temperature_2m,weather_code"))
                .andRespond(withSuccess("""
                        {"current":{"temperature_2m":22.4,"weather_code":61}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(containsString("nominatim.openstreetmap.org/reverse")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("lat", "-23.5505"))
                .andExpect(queryParam("lon", "-46.6333"))
                .andExpect(queryParam("format", "jsonv2"))
                .andExpect(header("User-Agent", containsString("PlannerFy/1.0")))
                .andRespond(withSuccess("""
                        {"address":{"town":"São Paulo"}}
                        """, MediaType.APPLICATION_JSON));

        WeatherResponseDTO response = weatherService.getWeather(-23.5505, -46.6333);

        assertEquals(22.4, response.temperature());
        assertEquals("São Paulo", response.city());
        assertEquals("Chuva", response.condition());
        server.verify();
    }

    @Test
    void keepsTheWeatherWhenNominatimRejectsTheRequest() {
        server.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess("""
                        {"current":{"temperature_2m":18.0,"weather_code":0}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("nominatim.openstreetmap.org/reverse")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        WeatherResponseDTO response = weatherService.getWeather(-23.5505, -46.6333);

        assertEquals(18.0, response.temperature());
        assertNull(response.city());
        assertEquals("Céu limpo", response.condition());
        server.verify();
    }

    @Test
    void returnsNullCityWhenNominatimFindsNoAddress() {
        server.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess("""
                        {"current":{"temperature_2m":25.0,"weather_code":3}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("nominatim.openstreetmap.org/reverse")))
                .andRespond(withSuccess("""
                        {"error":"Unable to geocode"}
                        """, MediaType.APPLICATION_JSON));

        assertNull(weatherService.getWeather(0, 0).city());
        server.verify();
    }

    @Test
    void reportsBadGatewayWhenOpenMeteoFails() {
        server.expect(requestTo(containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withServerError());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> weatherService.getWeather(-23.5505, -46.6333));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        server.verify();
    }
}
