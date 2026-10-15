package com.workspace.api.pages.home.weather.service;

import com.workspace.api.pages.home.weather.dto.NominatimDTO;
import com.workspace.api.pages.home.weather.dto.OpenMeteoDTO;
import com.workspace.api.pages.home.weather.dto.WeatherResponseDTO;
import com.workspace.api.pages.home.weather.validation.WeatherValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Stream;

@Slf4j
@Service
public class WeatherService {
    // Nominatim's usage policy requires a User-Agent that identifies the application.
    private static final String NOMINATIM_USER_AGENT =
            "PlannerFy/1.0 (+https://github.com/alejandrofonsecac/api-workspace)";

    private final RestClient restClient;
    private final WeatherValidator weatherValidator;

    public WeatherService(RestClient.Builder builder, WeatherValidator weatherValidator) {
        this.restClient = builder.build();
        this.weatherValidator = weatherValidator;
    }

    public WeatherResponseDTO getWeather(double latitude, double longitude) {
        weatherValidator.validateCoordinates(latitude, longitude);

        OpenMeteoDTO weather;
        try {
            weather = fetchWeather(latitude, longitude);
        } catch (RestClientException exception) {
            log.warn("Open-Meteo request failed: {}", exception.toString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível consultar o servidor de clima.");
        }
        weatherValidator.validateWeatherResponse(weather);

        return new WeatherResponseDTO(
                weather.current().temperature2m(),
                findCity(latitude, longitude),
                convertWeatherCode(weather.current().weatherCode())
        );
    }

    /** The city is complementary: a geocoding failure must not hide the weather. */
    private String findCity(double latitude, double longitude) {
        try {
            NominatimDTO location = fetchLocation(latitude, longitude);
            if (location == null || location.address() == null) {
                log.warn("Nominatim returned no address for the requested coordinates");
                return null;
            }
            return resolveCity(location.address());
        } catch (RestClientException exception) {
            log.warn("Nominatim request failed: {}", exception.toString());
            return null;
        }
    }

    private OpenMeteoDTO fetchWeather(double latitude, double longitude) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.open-meteo.com")
                        .path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("current", "temperature_2m,weather_code")
                        .build())
                .retrieve()
                .body(OpenMeteoDTO.class);
    }

    private NominatimDTO fetchLocation(double latitude, double longitude) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("nominatim.openstreetmap.org")
                        .path("/reverse")
                        .queryParam("lat", latitude)
                        .queryParam("lon", longitude)
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", 1)
                        .build())
                .header("User-Agent", NOMINATIM_USER_AGENT)
                .retrieve()
                .body(NominatimDTO.class);
    }

    private String resolveCity(NominatimDTO.Address address) {
        return Stream.of(
                        address.city(),
                        address.town(),
                        address.village(),
                        address.municipality(),
                        address.county(),
                        address.state())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String convertWeatherCode(int weatherCode) {
        return switch (weatherCode) {
            case 0 -> "Céu limpo";
            case 1, 2 -> "Parcialmente nublado";
            case 3 -> "Nublado";
            case 45, 48 -> "Nevoeiro";
            case 51, 53, 55 -> "Garoa";
            case 56, 57 -> "Garoa congelante";
            case 61, 63, 65 -> "Chuva";
            case 66, 67 -> "Chuva congelante";
            case 71, 73, 75, 77 -> "Neve";
            case 80, 81, 82 -> "Pancadas de chuva";
            case 85, 86 -> "Pancadas de neve";
            case 95 -> "Trovoada";
            case 96, 99 -> "Trovoada com granizo";
            default -> "Condição desconhecida";
        };
    }
}
