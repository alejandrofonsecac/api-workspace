package com.workspace.api.home.weather.controller;

import com.workspace.api.home.weather.dto.WeatherResponseDTO;
import com.workspace.api.home.weather.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weather")
public class WeatherController {
    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public WeatherResponseDTO getWeather(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return weatherService.getWeather(latitude, longitude);
    }
}
