//package com.allegro.dicomback.service;
//
//import com.allegro.dicomback.dto.OrthancSeriesDto;
//import com.allegro.dicomback.dto.OrthancStudyDto;
//import com.allegro.dicomback.entity.Patient;
//import com.allegro.dicomback.entity.Series;
//import com.allegro.dicomback.entity.Study;
//import com.allegro.dicomback.repository.PatientRepository;
//import com.allegro.dicomback.repository.SeriesRepository;
//import com.allegro.dicomback.repository.StudyRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class DicomSyncHelperService {
//    private final StudyRepository studyRepository;
//    private final SeriesRepository seriesRepository;
//    private final PatientRepository patientRepository;
//    private final RestTemplate restTemplate;
//
//    private final String ORTHANC_URL = "http://localhost:8042";
//
//    @Transactional
//    public void syncStudy(String studyUuid) {
//        log.info("-> syncStudy 진입: {}", studyUuid); // 추가
//
//        try {
//            OrthancStudyDto dto = restTemplate.getForObject(ORTHANC_URL + "/studies/" + studyUuid + "?expand=true", OrthancStudyDto.class);
//            if (dto == null) {
//                log.warn("DTO가 null입니다. UUID: {}", studyUuid);
//                return;
//            }
//
//            if(dto.getMainDicomTags() == null) {
//                log.warn("MainDicomTags가 null입니다. UUID: {}", studyUuid);
//                return;
//            }
//
//            log.info("조회된 StudyInstanceUID: {}", dto.getMainDicomTags().getStudyInstanceUID());
//
////            String patientKey = dto.getPatientMainDicomTags().getPatientID();
////            Patient patient = patientRepository.findById(patientKey)
////                    .orElseThrow(() -> new RuntimeException("환자 정보를 찾을 수 없습니다."));
//
//
//            if (dto.getPatientMainDicomTags() == null || dto.getPatientMainDicomTags().getPatientID() == null) {
//                log.warn("PatientMainDicomTags가 없습니다. UUID: {}", studyUuid);
//                return;
//            }
//
//            String patientKey = dto.getPatientMainDicomTags().getPatientID();
//            // 필요한 초기값을 명시적으로 지정해줌
//            // 신규 생성 시에만 실행되는 분기
//            Patient patient = patientRepository.findById(patientKey)
//                    .orElseThrow(() -> new RuntimeException("환자 정보를 찾을 수 없습니다."));
//
//            // 1. Study Upsert
//            String studyUid = dto.getMainDicomTags().getStudyInstanceUID();
//            Study study = studyRepository.findByStudyInstanceUid(studyUid)
//                    .orElse(Study.builder().studyInstanceUid(studyUid).build());
//
//            study.setPatient(patient);
//            study.setOrthancStudyId(dto.getID());
//            study.setStudyDateTime(convertToLocalDateTime(dto.getMainDicomTags().getStudyDate(), dto.getMainDicomTags().getStudyTime()));
//            study.setDescription(dto.getMainDicomTags().getStudyDescription());
//            study.setAccessionNumber(dto.getMainDicomTags().getAccessionNumber());
//            study.setTotalSeriesCount(dto.getSeries() != null ? dto.getSeries().size() : 0);
//
//            studyRepository.save(study);
//
//            log.info("기존 환자 PTime: {}", patient.getRecentStudy());
//            log.info("새로 계산된 StudyDateTime: {}", study.getStudyDateTime());
//
//            if(study.getStudyDateTime() == null) {
//                log.info("검사 날짜가 null입니다.");
//                return;
//            }
//
//            // 저장된 studies 테이블을 바탕으로 환자 최근 검사일 업데이트
//            patient.setRecentStudy(study.getStudyDateTime());
//            log.info("환자 {}의 최신 진료일 업데이트 완료: {}", patient.getKey(), patient.getRecentStudy());
//
//            patientRepository.save(patient);
//
//            // 2. Series Upsert 루프
//            if (dto.getSeries() != null) {
//                int totalImagesInStudy = 0;
//
//                for (String seriesUid : dto.getSeries()) {
//                    OrthancSeriesDto sDto = null;
//
//                    try {
//                        // 각 시리즈별 상세 정보 조회 (Orthanc API 호출)
//                        sDto = restTemplate.getForObject(ORTHANC_URL + "/series/" + seriesUid + "?expand=true", OrthancSeriesDto.class);
//                    } catch (Exception e) {
//                        log.error("Failed to sync series {}: {}", seriesUid, e.getMessage());
//                    }
//
//                    if (sDto == null) continue;
//
//                    String sUid = sDto.getMainDicomTags().getSeriesInstanceUID();
//                    Series series = seriesRepository.findBySeriesInstanceUid(sUid)
//                            .orElse(Series.builder().seriesInstanceUid(sUid).study(study).build());
//
//                    series.setOrthancId(sDto.getID());
//                    log.info("Series 번호: {}", sDto.getID());
//                    series.setSeriesNum(sDto.getMainDicomTags().getSeriesNumber());
//                    series.setModality(sDto.getMainDicomTags().getModality());
//                    series.setBodyPart(sDto.getMainDicomTags().getBodyPartExamined());
//
//                    // 이미지 개수 저장
//                    int instanceCount = (sDto.getInstances() != null) ? sDto.getInstances().size() : 0;
//                    series.setTotalInstanceCount(instanceCount);
//
//                    seriesRepository.save(series);
//                    totalImagesInStudy += instanceCount;
//                }
//
//                // Study의 총 이미지 카운트 업데이트
//                study.setTotalInstanceCount(totalImagesInStudy);
//                studyRepository.save(study);
//                log.info("저장 완료: {}", studyUid);
//            }
//        } catch (Exception e) {
//            log.error("저장 실패: {}", studyUuid, e);
//            throw e;
//        }
//    }
//
//    //DICOM의 Date/Time 표현 방식을 Java LocalDateTime으로 변환
//    //Dicom에서 받아온 데이터가 -> 20260912이기때문에 한번 변환한다.
//    private LocalDateTime convertToLocalDateTime(String dateStr, String timeStr) {
//        if (dateStr == null || dateStr.length() < 8) return LocalDateTime.now();
//        String time = (timeStr != null && !timeStr.isEmpty()) ? timeStr.split("\\.")[0] : "000000";
//        return LocalDateTime.parse(dateStr + time, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
//    }
//}
