//package com.allegro.dicomback.service;
//
//import com.allegro.dicomback.dto.OrthancPatientDto;
//import com.allegro.dicomback.entity.Patient;
//import com.allegro.dicomback.repository.PatientRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//import java.util.Arrays;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class PatientSyncService {
//
//    private final PatientRepository patientRepository;
//    private final RestTemplate restTemplate;
//
//    // Orthanc 서버 주소 (운영 환경에 따라 application.yml로 빼는 것을 추천)
//    private static final String ORTHANC_URL = "http://localhost:8042/patients?expand=true";
//
//    @Transactional
//    public void syncPatientsFromOrthanc() {
//        // 1. Orthanc에서 데이터 가져오기
//        OrthancPatientDto[] patientsArray = restTemplate.getForObject(ORTHANC_URL, OrthancPatientDto[].class);
//
//        if (patientsArray == null || patientsArray.length == 0) {
//            log.info("Orthanc 서버에 환자 데이터가 없습니다.");
//            return;
//        }
//
//        // 2. DTO를 Entity로 변환
//        List<Patient> patients = Arrays.stream(patientsArray)
//                .map(this::convertToEntity)
//                .collect(Collectors.toList());
//
//        // 3. DB에 일괄 저장 (이미 존재하는 PK는 덮어쓰거나 무시하도록 로직 추가 가능)
//        patientRepository.saveAll(patients);
//        log.info("{} 명의 환자 데이터 동기화 완료!", patients.size());
//    }
//
//    private Patient convertToEntity(OrthancPatientDto dto) {
//        OrthancPatientDto.MainDicomTags tags = dto.getMainDicomTags();
//
//        // 날짜 파싱 처리
//        LocalDateTime birthDate = parseBirthDate(tags.getPatientBirthDate());
//        LocalDateTime lastUpdate = parseLastUpdate(dto.getLastUpdate());
//
//        return Patient.builder()
//                // 엔티티의 pId(PK)에 DICOM PatientID를 넣습니다.
//                .id(tags.getPatientId() != null ? tags.getPatientId() : "UNKNOWN_ID")
//                .name(tags.getPatientName())
//                .birth(birthDate)
//                .sex(tags.getPatientSex())
//                .recentStudy(lastUpdate)
//                .studyCount(dto.getStudies() != null ? dto.getStudies().size() : 0)
//                .hiddenFlag(0)
//                .build();
//    }
//
//    // "19840119" -> LocalDateTime 변환 (시간은 00:00:00으로 세팅)
//    private LocalDateTime parseBirthDate(String dateStr) {
//        if (dateStr == null || dateStr.isBlank() || dateStr.equalsIgnoreCase("UNKNOWN")) {
//            return null;
//        }
//        try {
//            LocalDate date = LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("yyyyMMdd"));
//            return date.atStartOfDay();
//        } catch (DateTimeParseException e) {
//            log.warn("생년월일 파싱 실패: {}", dateStr);
//            return null;
//        }
//    }
//
//    // "20260624T084250" -> LocalDateTime 변환
//    private LocalDateTime parseLastUpdate(String dateTimeStr) {
//        if (dateTimeStr == null || dateTimeStr.isBlank()) {
//            return LocalDateTime.now();
//        }
//        try {
//            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
//        } catch (DateTimeParseException e) {
//            log.warn("LastUpdate 파싱 실패: {}", dateTimeStr);
//            return LocalDateTime.now();
//        }
//    }
//}