package com.allegro.backanonymization.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OrthancConfig {

    @Bean
    public WebClient orthancWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://localhost:8042")
                .defaultHeaders(headers -> {
//                    headers.setBasicAuth("", ""); // 인증 안 쓰면 제거
                })
                .build();
    }
}