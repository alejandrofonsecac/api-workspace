package com.workspace.api.home.weather.dto;

public record WeatherResponseDTO (
        double temperature,
        String city,
        String condition
){
}
