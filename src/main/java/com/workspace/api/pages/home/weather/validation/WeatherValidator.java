package com.workspace.api.pages.home.weather.validation;

import com.workspace.api.pages.home.weather.dto.OpenMeteoDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class WeatherValidator {

    public void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Latitude deve estar entre -90 e 90 e longitude entre -180 e 180.");
        }
    }

    public void validateWeatherResponse(OpenMeteoDTO weather) {
        if (weather == null || weather.current() == null
                || weather.current().temperature2m() == null || weather.current().weatherCode() == null) {
            throw upstreamFailure("A Open-Meteo não retornou os dados climáticos esperados.");
        }
    }

    private ResponseStatusException upstreamFailure(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason);
    }
}
