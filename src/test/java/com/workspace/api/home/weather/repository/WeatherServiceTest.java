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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

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
    private static final String OPEN_METEO = "api.open-meteo.com/v1/forecast";
    private static final String MET_NORWAY = "api.met.no/weatherapi/locationforecast/2.0/compact";
    private static final String NOMINATIM = "nominatim.openstreetmap.org/reverse";

    private MockRestServiceServer server;
    private MutableClock clock;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.createServer(builder);
        clock = new MutableClock(Instant.parse("2026-10-15T13:00:00Z"));
        weatherService = new WeatherService(builder, new WeatherValidator(), clock);
    }

    @Test
    void getsCurrentWeatherAndResolvesTheCityFromNominatim() {
        server.expect(requestTo(containsString(OPEN_METEO)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("latitude", "-23.5505"))
                .andExpect(queryParam("longitude", "-46.6333"))
                .andExpect(queryParam("current", "temperature_2m,weather_code"))
                .andRespond(withSuccess("""
                        {"current":{"temperature_2m":22.4,"weather_code":61}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(containsString(NOMINATIM)))
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
        expectOpenMeteo(18.0, 0);
        server.expect(requestTo(containsString(NOMINATIM)))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        WeatherResponseDTO response = weatherService.getWeather(-23.5505, -46.6333);

        assertEquals(18.0, response.temperature());
        assertNull(response.city());
        assertEquals("Céu limpo", response.condition());
        server.verify();
    }

    @Test
    void returnsNullCityWhenNominatimFindsNoAddress() {
        expectOpenMeteo(25.0, 3);
        server.expect(requestTo(containsString(NOMINATIM)))
                .andRespond(withSuccess("""
                        {"error":"Unable to geocode"}
                        """, MediaType.APPLICATION_JSON));

        assertNull(weatherService.getWeather(0, 0).city());
        server.verify();
    }

    @Test
    void fallsBackToMetNorwayWhenOpenMeteoFails() {
        server.expect(requestTo(containsString(OPEN_METEO))).andRespond(withServerError());
        server.expect(requestTo(containsString(MET_NORWAY)))
                .andExpect(queryParam("lat", "-26.7381"))
                .andExpect(queryParam("lon", "-49.2725"))
                .andExpect(header("User-Agent", containsString("PlannerFy/1.0")))
                .andRespond(withSuccess(metNorway(23.8, "lightrainshowers_day"), MediaType.APPLICATION_JSON));
        expectCity("Blumenau");

        WeatherResponseDTO response = weatherService.getWeather(-26.73812345, -49.27254321);

        assertEquals(23.8, response.temperature());
        assertEquals("Blumenau", response.city());
        assertEquals("Pancadas de chuva", response.condition());
        server.verify();
    }

    @Test
    void pausesOpenMeteoAfterItsQuotaIsExceeded() {
        server.expect(requestTo(containsString(OPEN_METEO))).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(containsString(MET_NORWAY)))
                .andRespond(withSuccess(metNorway(20.0, "cloudy"), MediaType.APPLICATION_JSON));
        expectCity("São Paulo");
        // Different coordinates bypass the cache; the strict request order proves Open-Meteo is skipped.
        server.expect(requestTo(containsString(MET_NORWAY)))
                .andRespond(withSuccess(metNorway(26.0, "clearsky_day"), MediaType.APPLICATION_JSON));
        expectCity("Rio de Janeiro");

        assertEquals("Nublado", weatherService.getWeather(-23.5505, -46.6333).condition());
        assertEquals("Céu limpo", weatherService.getWeather(-22.9068, -43.1729).condition());
        server.verify();
    }

    @Test
    void reportsBadGatewayWhenEveryWeatherProviderFails() {
        server.expect(requestTo(containsString(OPEN_METEO))).andRespond(withServerError());
        server.expect(requestTo(containsString(MET_NORWAY))).andRespond(withServerError());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> weatherService.getWeather(-23.5505, -46.6333));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        server.verify();
    }

    @Test
    void reusesTheWeatherOfNearbyCoordinatesUntilTheCacheExpires() {
        expectOpenMeteo(22.0, 0);
        expectCity("São Paulo");

        WeatherResponseDTO first = weatherService.getWeather(-23.5505, -46.6333);
        clock.advance(Duration.ofMinutes(9));
        WeatherResponseDTO cached = weatherService.getWeather(-23.5512, -46.6341);
        server.verify();
        assertEquals(first, cached);

        server.reset();
        expectOpenMeteo(24.0, 0);
        expectCity("São Paulo");
        clock.advance(Duration.ofMinutes(2));

        assertEquals(24.0, weatherService.getWeather(-23.5505, -46.6333).temperature());
        server.verify();
    }

    private void expectOpenMeteo(double temperature, int weatherCode) {
        server.expect(requestTo(containsString(OPEN_METEO)))
                .andRespond(withSuccess("""
                        {"current":{"temperature_2m":%s,"weather_code":%d}}
                        """.formatted(temperature, weatherCode), MediaType.APPLICATION_JSON));
    }

    private void expectCity(String city) {
        server.expect(requestTo(containsString(NOMINATIM)))
                .andRespond(withSuccess("""
                        {"address":{"city":"%s"}}
                        """.formatted(city), MediaType.APPLICATION_JSON));
    }

    private static String metNorway(double temperature, String symbolCode) {
        return """
                {"properties":{"timeseries":[{"time":"2026-10-15T13:00:00Z","data":{
                  "instant":{"details":{"air_temperature":%s}},
                  "next_1_hours":{"summary":{"symbol_code":"%s"}},
                  "next_6_hours":{"summary":{"symbol_code":"rain"}}}}]}}
                """.formatted(temperature, symbolCode);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
