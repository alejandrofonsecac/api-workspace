package com.workspace.api.pages.home.weather.dto;

/** {@code city} is null when the coordinates could not be resolved to a place. */
public record WeatherResponseDTO (
        double temperature,
        String city,
        String condition
){
}
