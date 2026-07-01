package com.allegro.dicomback.test;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OrthancTestRunner implements CommandLineRunner {

    private final OrthancDebugService orthancDebugService;

    public OrthancTestRunner(OrthancDebugService orthancDebugService) {
        this.orthancDebugService = orthancDebugService;
    }

    @Override
    public void run(String... args) throws Exception {
        // TODO: Orthanc에 업로드한 실제 Instance UUID를 여기에 넣으세요.
        String testInstanceId = "1dbd66a4-72403921-9320a634-13e01b53-4f2b8ca1";

        System.out.println(">>> Orthanc DICOM 메타데이터 조회 테스트를 시작합니다...");
        orthancDebugService.printDicomMetadata(testInstanceId);
    }
}