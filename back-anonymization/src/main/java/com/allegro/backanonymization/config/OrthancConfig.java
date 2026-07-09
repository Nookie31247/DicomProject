package com.allegro.backanonymization.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OrthancConfig {

    @Bean
    public WebClient orthancWebClient(
            WebClient.Builder builder,
            @Value("${orthanc.url}") String orthancUrl
    ) {
        return builder
                .baseUrl(orthancUrl)
                .build();
    }
}
