package com.workspace.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    //Este RestClient e para configurar a chamada da API de weather em HOME

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
