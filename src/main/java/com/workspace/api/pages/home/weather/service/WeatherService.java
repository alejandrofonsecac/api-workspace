package com.workspace.api.pages.home.weather.service;

import com.workspace.api.pages.home.weather.dto.MetNorwayDTO;
import com.workspace.api.pages.home.weather.dto.NominatimDTO;
import com.workspace.api.pages.home.weather.dto.OpenMeteoDTO;
import com.workspace.api.pages.home.weather.dto.WeatherResponseDTO;
import com.workspace.api.pages.home.weather.validation.WeatherValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@Service
public class WeatherService {
    // Nominatim and MET Norway require a User-Agent that identifies the application.
    private static final String USER_AGENT =
            "PlannerFy/1.0 (+https://github.com/alejandrofonsecac/api-workspace)";

    // Two decimals (~1 km) is precise enough for current weather and lets nearby users share a result.
    private static final double CACHE_PRECISION = 100;
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final int CACHE_MAX_ENTRIES = 1_000;

    // Open-Meteo's free quota is per IP, and hosting providers share outbound IPs between customers.
    // Once it answers 429 the quota is gone for everyone on that IP, so stop asking for a while.
    private static final Duration OPEN_METEO_PAUSE = Duration.ofHours(1);

    private final RestClient restClient;
    private final WeatherValidator weatherValidator;
    private final Clock clock;
    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();
    private volatile Instant openMeteoPausedUntil = Instant.MIN;

    public WeatherService(RestClient.Builder builder, WeatherValidator weatherValidator, Clock clock) {
        this.restClient = builder.build();
        this.weatherValidator = weatherValidator;
        this.clock = clock;
    }

    public WeatherResponseDTO getWeather(double latitude, double longitude) {
        weatherValidator.validateCoordinates(latitude, longitude);

        String key = cacheKey(latitude, longitude);
        CachedWeather cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(clock.instant())) {
            return cached.weather();
        }

        CurrentConditions conditions = fetchCurrentConditions(latitude, longitude);
        WeatherResponseDTO weather = new WeatherResponseDTO(
                conditions.temperature(),
                findCity(latitude, longitude),
                conditions.condition()
        );
        store(key, weather);
        return weather;
    }

    /** Open-Meteo first; MET Norway when it fails or is paused. */
    private CurrentConditions fetchCurrentConditions(double latitude, double longitude) {
        if (clock.instant().isAfter(openMeteoPausedUntil)) {
            try {
                OpenMeteoDTO weather = fetchOpenMeteo(latitude, longitude);
                weatherValidator.validateWeatherResponse(weather);
                return new CurrentConditions(weather.current().temperature2m(),
                        convertWeatherCode(weather.current().weatherCode()));
            } catch (HttpClientErrorException.TooManyRequests exception) {
                openMeteoPausedUntil = clock.instant().plus(OPEN_METEO_PAUSE);
                log.warn("Open-Meteo quota exceeded, using MET Norway until {}", openMeteoPausedUntil);
            } catch (RestClientException | ResponseStatusException exception) {
                log.warn("Open-Meteo request failed, using MET Norway: {}", exception.toString());
            }
        }

        try {
            MetNorwayDTO forecast = fetchMetNorway(latitude, longitude);
            weatherValidator.validateMetNorwayResponse(forecast);
            MetNorwayDTO.Data current = forecast.properties().timeseries().getFirst().data();
            MetNorwayDTO.Period period = current.next1Hours() != null && current.next1Hours().summary() != null
                    ? current.next1Hours() : current.next6Hours();
            return new CurrentConditions(current.instant().details().airTemperature(),
                    convertSymbolCode(period.summary().symbolCode()));
        } catch (RestClientException exception) {
            log.warn("MET Norway request failed: {}", exception.toString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível consultar os servidores de clima.");
        }
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

    private OpenMeteoDTO fetchOpenMeteo(double latitude, double longitude) {
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

    private MetNorwayDTO fetchMetNorway(double latitude, double longitude) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.met.no")
                        .path("/weatherapi/locationforecast/2.0/compact")
                        // MET Norway rejects coordinates with more than four decimals.
                        .queryParam("lat", round(latitude, 10_000))
                        .queryParam("lon", round(longitude, 10_000))
                        .build())
                .header("User-Agent", USER_AGENT)
                .retrieve()
                .body(MetNorwayDTO.class);
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
                .header("User-Agent", USER_AGENT)
                .retrieve()
                .body(NominatimDTO.class);
    }

    private void store(String key, WeatherResponseDTO weather) {
        Instant now = clock.instant();
        if (cache.size() >= CACHE_MAX_ENTRIES) {
            cache.values().removeIf(entry -> !entry.expiresAt().isAfter(now));
            if (cache.size() >= CACHE_MAX_ENTRIES) {
                cache.clear();
            }
        }
        cache.put(key, new CachedWeather(weather, now.plus(CACHE_TTL)));
    }

    private String cacheKey(double latitude, double longitude) {
        return round(latitude, CACHE_PRECISION) + "," + round(longitude, CACHE_PRECISION);
    }

    private double round(double value, double scale) {
        return Math.round(value * scale) / scale;
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

    /** Maps MET Norway symbol codes (e.g. "lightrainshowers_day") to the same labels as Open-Meteo. */
    private String convertSymbolCode(String symbolCode) {
        String symbol = symbolCode.replaceFirst("_(day|night|polartwilight)$", "");
        if (symbol.contains("thunder")) {
            return "Trovoada";
        }
        if (symbol.contains("showers")) {
            if (symbol.contains("rain")) {
                return "Pancadas de chuva";
            }
            if (symbol.contains("snow")) {
                return "Pancadas de neve";
            }
        }
        if (symbol.contains("sleet")) {
            return "Chuva congelante";
        }
        if (symbol.contains("rain")) {
            return "Chuva";
        }
        if (symbol.contains("snow")) {
            return "Neve";
        }
        return switch (symbol) {
            case "clearsky" -> "Céu limpo";
            case "fair", "partlycloudy" -> "Parcialmente nublado";
            case "cloudy" -> "Nublado";
            case "fog" -> "Nevoeiro";
            default -> "Condição desconhecida";
        };
    }

    private record CurrentConditions(double temperature, String condition) {
    }

    private record CachedWeather(WeatherResponseDTO weather, Instant expiresAt) {
    }
}
