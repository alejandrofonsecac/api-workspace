package com.workspace.api.pages.home.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Compact forecast from MET Norway (api.met.no locationforecast 2.0). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetNorwayDTO(Properties properties) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(List<TimeStep> timeseries) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimeStep(Data data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            InstantData instant,
            @JsonProperty("next_1_hours") Period next1Hours,
            @JsonProperty("next_6_hours") Period next6Hours
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InstantData(Details details) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Details(@JsonProperty("air_temperature") Double airTemperature) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Period(Summary summary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(@JsonProperty("symbol_code") String symbolCode) {
    }
}
