package com.allegro.dicomback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DICOM 백엔드를 위한 메인 애플리케이션 클래스입니다.
 */
@SpringBootApplication
public class DicomBackApplication {

    /**
     * 애플리케이션의 진입점입니다.
     *
     * @param args 명령줄 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(DicomBackApplication.class, args);
    }

}
