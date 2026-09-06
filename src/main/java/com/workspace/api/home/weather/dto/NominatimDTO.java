package com.workspace.api.home.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimDTO(Address address) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            String city,
            String town,
            String village,
            String municipality,
            String county,
            String state
    ) {
    }
}
