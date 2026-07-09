package com.allegro.backanonymization.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * 애플리케이션 설정 클래스입니다.
 */
@Configuration
@EnableAsync
public class AppConfig {
    /**
     * RestTemplate 빈을 제공합니다.
     *
     * @return 새로운 RestTemplate 인스턴스
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}