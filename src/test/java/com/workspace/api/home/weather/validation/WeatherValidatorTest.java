package com.workspace.api.home.weather.validation;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeatherValidatorTest {
    private final WeatherValidator validator = new WeatherValidator();

    @Test
    void rejectsCoordinatesOutsideTheSupportedRange() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> validator.validateCoordinates(-90.1, -46.6333));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void rejectsAnIncompleteOpenMeteoResponse() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> validator.validateWeatherResponse(null));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    }
}
