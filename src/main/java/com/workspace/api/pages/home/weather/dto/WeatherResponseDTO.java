package com.workspace.api.pages.home.weather.dto;

public record WeatherResponseDTO (
        double temperature,
        String city,
        String condition
){
}
