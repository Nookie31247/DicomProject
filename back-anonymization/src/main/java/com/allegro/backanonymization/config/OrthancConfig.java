package com.allegro.backanonymization.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Orthanc 연동을 위한 설정입니다.
 */
@Configuration
public class OrthancConfig {

    /**
     * Orthanc을 위한 WebClient 빈을 제공합니다.
     *
     * @param builder WebClient 빌더
     * @param orthancUrl Orthanc URL
     * @return 새로운 WebClient 인스턴스
     */
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
