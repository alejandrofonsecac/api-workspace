package com.workspace.api.pages.home.weather.service;

import com.workspace.api.pages.home.weather.dto.NominatimDTO;
import com.workspace.api.pages.home.weather.dto.OpenMeteoDTO;
import com.workspace.api.pages.home.weather.dto.WeatherResponseDTO;
import com.workspace.api.pages.home.weather.validation.WeatherValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Stream;

@Service
public class WeatherService {
    private static final String NOMINATIM_USER_AGENT = "PlannerFy/1.0";

    private final RestClient restClient;
    private final WeatherValidator weatherValidator;

    public WeatherService(RestClient.Builder builder, WeatherValidator weatherValidator) {
        this.restClient = builder.build();
        this.weatherValidator = weatherValidator;
    }

    public WeatherResponseDTO getWeather(double latitude, double longitude) {
        weatherValidator.validateCoordinates(latitude, longitude);

        try {
            OpenMeteoDTO weather = fetchWeather(latitude, longitude);
            NominatimDTO location = fetchLocation(latitude, longitude);

            weatherValidator.validateWeatherResponse(weather);
            weatherValidator.validateLocationResponse(location);

            return new WeatherResponseDTO(
                    weather.current().temperature2m(),
                    resolveCity(location.address()),
                    convertWeatherCode(weather.current().weatherCode())
            );
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível consultar os servidores de clima e localização.");
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
                .orElse("Localização desconhecida");
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
