package com.allegro.backanonymization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BackAnonymization을 위한 메인 애플리케이션 클래스입니다.
 */
@SpringBootApplication
@EnableJpaAuditing
public class BackAnonymizationApplication {

    /**
     * 애플리케이션을 실행하기 위한 메인 메서드입니다.
     *
     * @param args 명령줄 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(BackAnonymizationApplication.class, args);
    }

}
