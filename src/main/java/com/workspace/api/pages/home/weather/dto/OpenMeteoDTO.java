package com.workspace.api.pages.home.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoDTO(CurrentWeather current) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentWeather(
            @JsonProperty("temperature_2m") Double temperature2m,
            @JsonProperty("weather_code") Integer weatherCode
    ) {
    }
}
